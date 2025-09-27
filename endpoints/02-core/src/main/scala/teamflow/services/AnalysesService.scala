package teamflow.services

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Concurrent
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import teamflow.domain.AgentId
import teamflow.domain.AnalysisId
import teamflow.domain.PaginatedResponse
import teamflow.domain.ProjectId
import teamflow.domain.Response
import teamflow.domain.UserId
import teamflow.domain.agents.Agent
import teamflow.domain.analyses.Analysis
import teamflow.domain.analyses.AnalysisFilter
import teamflow.domain.analyses.AnalysisInput
import teamflow.domain.auth.AuthedUser
import teamflow.domain.projects.Project
import teamflow.domain.users.UserFilter
import teamflow.effects.Calendar
import teamflow.effects.GenUUID
import teamflow.exception.AError
import teamflow.integrations.anthropic.AnthropicClient
import teamflow.integrations.github.GithubClient
import teamflow.integrations.github.domain.commits.CommitDetails
import teamflow.integrations.github.domain.commits.{ Response => GitHubResponse }
import teamflow.repositories.AgentsRepository
import teamflow.repositories.AnalysesRepository
import teamflow.repositories.ProjectsRepository
import teamflow.repositories.UsersRepository
import teamflow.utils.ID

trait AnalysesService[F[_]] {
  def analyze(input: AnalysisInput): F[Response]
  def get(filters: AnalysisFilter): F[PaginatedResponse[Analysis]]
  def find(id: AnalysisId): F[Option[Analysis]]
  def findByUserId(userId: UserId): F[List[Analysis]]
  def delete(id: AnalysisId): F[Response]
  def linkUserToAnalysis(userId: UserId, analysisId: AnalysisId): F[Response]
  def unlinkUserFromAnalysis(userId: UserId, analysisId: AnalysisId): F[Response]
}

object AnalysesService {
  def make[F[_]: Concurrent: GenUUID: Calendar](
      analysesRepo: AnalysesRepository[F],
      projectsRepo: ProjectsRepository[F],
      agentsRepo: AgentsRepository[F],
      usersRepo: UsersRepository[F],
      githubClient: GithubClient[F],
      anthropicClient: AnthropicClient[F],
    ): AnalysesService[F] =
    new AnalysesService[F] {
      override def analyze(input: AnalysisInput): F[Response] =
        for {
          id <- ID.make[F, AnalysisId]
          _ <- Concurrent[F].start(processAnalysis(id, input)).void
        } yield Response(id.value, "Analysis started")

      override def get(filters: AnalysisFilter): F[PaginatedResponse[Analysis]] =
        analysesRepo.get(filters)

      override def find(id: AnalysisId): F[Option[Analysis]] =
        analysesRepo.findById(id)

      override def findByUserId(userId: UserId): F[List[Analysis]] =
        analysesRepo.findByUserId(userId)

      override def delete(id: AnalysisId): F[Response] =
        OptionT(find(id))
          .semiflatMap { _ =>
            analysesRepo.delete(id) >>
              Response(id.value, "Analysis deleted").pure[F]
          }
          .getOrElse(Response(id.value, s"Analysis with id $id not found"))

      override def linkUserToAnalysis(userId: UserId, analysisId: AnalysisId): F[Response] =
        analysesRepo.linkUserToAnalysis(userId, analysisId) >>
          Response(analysisId.value, "User linked to analysis").pure[F]

      override def unlinkUserFromAnalysis(userId: UserId, analysisId: AnalysisId): F[Response] =
        analysesRepo.unlinkUserFromAnalysis(userId, analysisId) >>
          Response(analysisId.value, "User unlinked from analysis").pure[F]

      private def fetchTargetUsers(userIds: NonEmptyList[UserId]): F[List[AuthedUser.User]] =
        usersRepo.get(UserFilter()).map(_.data.map(u => u.id -> u).toMap).map { allUsers =>
          userIds.toList.flatMap(userId => allUsers.get(userId))
        }

      private def fetchProjectInfo(projectId: ProjectId): F[Project] =
        OptionT(projectsRepo.findById(projectId))
          .getOrElseF(
            Concurrent[F].raiseError[Project](
              AError.BadRequest(s"Project with id $projectId not found")
            )
          )

      private def fetchAgentInfo(agentId: AgentId): F[Agent] =
        OptionT(agentsRepo.findById(agentId))
          .getOrElseF(
            Concurrent[F].raiseError[Agent](
              AError.BadRequest(s"Agent with id $agentId not found")
            )
          )

      private def fetchRelevantCommits(
          project: Project,
          targetUsers: List[AuthedUser.User],
        ): F[List[GitHubResponse]] =
        for {
          allCommits <- githubClient.getCommits(project.name)
          targetUserEmails = targetUsers.map(_.email).toSet
          filteredCommits = allCommits.filter { commit =>
            targetUserEmails.contains(commit.commit.author.email) ||
            targetUserEmails.contains(commit.commit.committer.email)
          }
          limitedCommits = filteredCommits.take(10)
        } yield limitedCommits

      private def fetchCommitDetails(
          project: Project,
          commits: List[GitHubResponse],
        ): F[List[CommitDetails]] =
        commits.traverse(commit => githubClient.getCommitDetails(project.name, commit.sha))

      private def buildAnalysisPrompt(
          commitDetails: List[CommitDetails],
          targetUsers: List[AuthedUser.User],
          agent: Agent,
        ): String = {
        val userInfo = targetUsers
          .map(u => s"- ${u.firstName} ${u.lastName} (${u.email})")
          .mkString("\n")

        val commitInfo = commitDetails
          .map { commit =>
            val limitedFiles = commit.files.take(5).filter(_.changes <= 200)
            val fileChanges = limitedFiles
              .map { file =>
                s"""File: ${file.filename}
               |Status: ${file.status}
               |Changes: +${file.additions} -${file.deletions}
               |${file.patch.map(p => s"Patch:\n$p").getOrElse("")}
               |""".stripMargin
              }
              .mkString("\n---\n")

            s"""Commit: ${commit.sha.take(8)}
             |Author: ${commit.commit.author.name} <${commit.commit.author.email}>
             |Date: ${commit.commit.author.date}
             |Message: ${commit.commit.message}
             |Files Changed:
             |$fileChanges
             |Stats: +${commit.stats.additions} -${commit.stats.deletions}
             |""".stripMargin
          }
          .mkString("\n" + "=" * 60 + "\n")

        s"""${agent.prompt.value}

           |Please analyze the following code commits made by these developers:

           |Target Developers:
           |$userInfo

           |Code Commits to Analyze:
           |$commitInfo

           |Please provide a comprehensive analysis including:
           |1. Code quality assessment
           |2. Coding patterns and practices used  
           |3. Potential issues or improvements
           |4. Summary of changes and their impact
           |5. Developer-specific insights and recommendations

           |Focus on constructive feedback and actionable insights.
           |""".stripMargin
      }

      private def runAIAnalysis(prompt: String): F[String] =
        for {
          response <- anthropicClient.sendMessage(
            message = prompt,
            maxTokens = Some(4000),
          )
          analysisResult = response
            .content
            .filter(_.`type` == "text")
            .map(_.text)
            .mkString("\n")
        } yield analysisResult

      private def saveAnalysisResult(
          id: AnalysisId,
          projectId: ProjectId,
          agentId: AgentId,
          result: String,
        ): F[Unit] =
        for {
          now <- Calendar[F].currentZonedDateTime
          analysis = Analysis(
            id = id,
            createdAt = now,
            projectId = projectId,
            agentId = agentId,
            response = NonEmptyString.unsafeFrom(result),
          )
          _ <- analysesRepo.create(analysis)
        } yield ()

      private def processAnalysis(id: AnalysisId, filter: AnalysisInput): F[Unit] =
        (for {
          targetUsers <- fetchTargetUsers(filter.userIds)
          project <- fetchProjectInfo(filter.projectId)
          agent <- fetchAgentInfo(filter.agentId)
          relevantCommits <- fetchRelevantCommits(project, targetUsers)
          commitDetails <- fetchCommitDetails(project, relevantCommits)
          prompt = buildAnalysisPrompt(commitDetails, targetUsers, agent)
          analysisResult <- runAIAnalysis(prompt)
          _ <- saveAnalysisResult(id, filter.projectId, filter.agentId, analysisResult)
        } yield ()).handleErrorWith { error =>
          val errorMessage = s"Analysis failed: ${error.getMessage}"
          for {
            _ <- saveAnalysisResult(id, filter.projectId, filter.agentId, errorMessage)
            _ <- AError.BadRequest(errorMessage).raiseError[F, Unit]
          } yield ()
        }
    }
}
