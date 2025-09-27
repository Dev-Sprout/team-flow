package teamflow.services

import java.time.LocalDate

import scala.concurrent.duration.DurationInt

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
import teamflow.domain.analyses.AnalysisInfo
import teamflow.domain.analyses.AnalysisInput
import teamflow.domain.auth.AuthedUser
import teamflow.domain.enums.AnalysisStatus
import teamflow.domain.projects.Project
import teamflow.domain.users.UserFilter
import teamflow.effects.Calendar
import teamflow.effects.GenUUID
import teamflow.exception.AError
import teamflow.integrations.anthropic.AnthropicClient
import teamflow.integrations.anthropic.domain.messages
import teamflow.integrations.github.GithubClient
import teamflow.integrations.github.domain.commits.CommitDetails
import teamflow.integrations.github.domain.commits.{ Response => GitHubResponse }
import teamflow.repositories.AgentsRepository
import teamflow.repositories.AnalysisRepository
import teamflow.repositories.ProjectsRepository
import teamflow.repositories.UsersRepository
import teamflow.support.redis.RedisClient
import teamflow.utils.ID

trait AnalysisService[F[_]] {
  def analyze(input: AnalysisInput): F[Response]
  def get(filters: AnalysisFilter): F[PaginatedResponse[AnalysisInfo]]
  def find(id: AnalysisId): F[Option[AnalysisInfo]]
  def check(id: AnalysisId): F[Option[AnalysisStatus]]
  def findByUserId(userId: UserId): F[List[AnalysisInfo]]
  def delete(id: AnalysisId): F[Response]
  def linkUserToAnalysis(userId: UserId, analysisId: AnalysisId): F[Response]
  def unlinkUserFromAnalysis(userId: UserId, analysisId: AnalysisId): F[Response]
}

object AnalysisService {
  def make[F[_]: Concurrent: GenUUID: Calendar](
      analysesRepo: AnalysisRepository[F],
      projectsRepo: ProjectsRepository[F],
      agentsRepo: AgentsRepository[F],
      usersRepo: UsersRepository[F],
      githubClient: GithubClient[F],
      anthropicClient: AnthropicClient[F],
      redisClient: RedisClient[F],
    ): AnalysisService[F] =
    new AnalysisService[F] {
      override def analyze(input: AnalysisInput): F[Response] =
        for {
          id <- ID.make[F, AnalysisId]
          _ <- Concurrent[F].start(processAnalysis(id, input)).void
          _ <- redisClient.put(s"analysis:$id", AnalysisStatus.Started.entryName, 30.minutes)
        } yield Response(id.value, AnalysisStatus.Started.entryName)

      override def get(filters: AnalysisFilter): F[PaginatedResponse[AnalysisInfo]] =
        for {
          analysisPage <- analysesRepo.get(filters)
          analyses = analysisPage.data

          // Collect unique IDs
          projectIds = analyses.map(_.projectId).distinct
          agentIds = analyses.map(_.agentId).distinct
          analysisIds = analyses.map(_.id)

          // Batch fetch all data
          projectsMap <- projectsRepo.findByIds(projectIds)
          agentsMap <- agentsRepo.findByIds(agentIds)
          usersMapByAnalysis <- analysisIds
            .traverse(id => analysesRepo.findUsersByAnalysisId(id).map(id -> _))
            .map(_.toMap)

          // Build AnalysisInfo objects
          analysisInfos = analyses.map { analysis =>
            AnalysisInfo(
              id = analysis.id,
              createdAt = analysis.createdAt,
              project = projectsMap(analysis.projectId),
              agent = agentsMap(analysis.agentId),
              users = usersMapByAnalysis.getOrElse(analysis.id, List.empty),
              response = analysis.response,
              dateFrom = analysis.dateFrom,
              dateTo = analysis.dateTo,
              durationSeconds = analysis.durationSeconds,
            )
          }
        } yield PaginatedResponse(analysisInfos, analysisPage.total)

      override def find(id: AnalysisId): F[Option[AnalysisInfo]] =
        analysesRepo.findById(id).flatMap {
          case Some(analysis) =>
            for {
              projectsMap <- projectsRepo.findByIds(List(analysis.projectId))
              agentsMap <- agentsRepo.findByIds(List(analysis.agentId))
              users <- analysesRepo.findUsersByAnalysisId(analysis.id)
            } yield Some(
              AnalysisInfo(
                id = analysis.id,
                createdAt = analysis.createdAt,
                project = projectsMap(analysis.projectId),
                agent = agentsMap(analysis.agentId),
                users = users,
                response = analysis.response,
                dateFrom = analysis.dateFrom,
                dateTo = analysis.dateTo,
                durationSeconds = analysis.durationSeconds,
              )
            )
          case None => Concurrent[F].pure(None)
        }

      override def check(id: AnalysisId): F[Option[AnalysisStatus]] =
        redisClient.get(s"analysis:$id").map(_.flatMap(s => AnalysisStatus.withNameOption(s)))

      override def findByUserId(userId: UserId): F[List[AnalysisInfo]] =
        for {
          analyses <- analysesRepo.findByUserId(userId)

          // Collect unique IDs
          projectIds = analyses.map(_.projectId).distinct
          agentIds = analyses.map(_.agentId).distinct
          analysisIds = analyses.map(_.id)

          // Batch fetch all data
          projectsMap <- projectsRepo.findByIds(projectIds)
          agentsMap <- agentsRepo.findByIds(agentIds)
          usersMapByAnalysis <- analysisIds
            .traverse(id => analysesRepo.findUsersByAnalysisId(id).map(id -> _))
            .map(_.toMap)

          // Build AnalysisInfo objects
          analysisInfos = analyses.map { analysis =>
            AnalysisInfo(
              id = analysis.id,
              createdAt = analysis.createdAt,
              project = projectsMap(analysis.projectId),
              agent = agentsMap(analysis.agentId),
              users = usersMapByAnalysis.getOrElse(analysis.id, List.empty),
              response = analysis.response,
              dateFrom = analysis.dateFrom,
              dateTo = analysis.dateTo,
              durationSeconds = analysis.durationSeconds,
            )
          }
        } yield analysisInfos

      override def delete(id: AnalysisId): F[Response] =
        OptionT(analysesRepo.findById(id))
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
          from: LocalDate,
          to: LocalDate,
        ): F[List[GitHubResponse]] =
        for {
          allCommits <- githubClient.getCommits(project.name, from, to)
          targetUserEmails = targetUsers.map(_.username.toString).toSet
          filteredCommits = allCommits.filter { commit =>
            targetUserEmails.contains(commit.author.login) ||
            targetUserEmails.contains(commit.committer.login)
          }
        } yield filteredCommits

      private def fetchCommitDetails(
          project: Project,
          commits: List[GitHubResponse],
        ): F[List[CommitDetails]] =
        commits.traverse(commit => githubClient.getCommitDetails(project.name, commit.sha))

      private def buildAnalysisPrompt(
          commitDetails: List[CommitDetails],
          targetUsers: List[AuthedUser.User],
        ): String = {
        val userInfo = targetUsers
          .map(u => s"- ${u.firstName} ${u.lastName} (${u.email})")
          .mkString("\n")

        // For comprehensive analysis, include more commits for monthly reports
        val limitedCommits = if (commitDetails.length > 50) commitDetails.take(50) else commitDetails
        
        val commitInfo = limitedCommits
          .map { commit =>
            val limitedFiles = commit.files.take(5).filter(_.changes <= 150)
            val fileChanges = limitedFiles
              .map { file =>
                // Truncate patch content to prevent huge prompts
                val truncatedPatch = file.patch.map { p =>
                  if (p.length > 800) s"${p.take(800)}...[truncated]"
                  else p
                }
                s"""File: ${file.filename}
               |Status: ${file.status}
               |Changes: +${file.additions} -${file.deletions}
               |${truncatedPatch.map(p => s"Patch:\n$p").getOrElse("")}
               |""".stripMargin
              }
              .mkString("\n---\n")

            s"""Commit: ${commit.sha.take(8)}
             |Author: ${commit.commit.author.name} <${commit.commit.author.email}>
             |Date: ${commit.commit.author.date}
             |Message: ${commit.commit.message.take(300)}${if (commit.commit.message.length > 300) "..." else ""}
             |Files Changed:
             |$fileChanges
             |Stats: +${commit.stats.additions} -${commit.stats.deletions}
             |""".stripMargin
          }
          .mkString("\n" + "=" * 60 + "\n")

        val basePrompt = s"""Please analyze the following code commits made by these developers:

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
           
        // If prompt is too long, truncate commit info further
        if (basePrompt.length > 180000) {
          val truncatedCommitInfo = limitedCommits.take(25)
            .map { commit =>
              s"""Commit: ${commit.sha.take(8)}
               |Author: ${commit.commit.author.name}
               |Message: ${commit.commit.message.take(100)}${if (commit.commit.message.length > 100) "..." else ""}
               |Files: ${commit.files.take(2).map(_.filename).mkString(", ")}
               |Stats: +${commit.stats.additions} -${commit.stats.deletions}
               |""".stripMargin
            }
            .mkString("\n" + "=" * 40 + "\n")
            
          s"""Please analyze the following code commits made by these developers:

             |Target Developers:
             |$userInfo

             |Code Commits to Analyze (summarized):
             |$truncatedCommitInfo

             |Please provide a comprehensive analysis including:
             |1. Code quality assessment
             |2. Coding patterns and practices used  
             |3. Potential issues or improvements
             |4. Summary of changes and their impact
             |5. Developer-specific insights and recommendations

             |Focus on constructive feedback and actionable insights.
             |""".stripMargin
        } else {
          basePrompt
        }
      }

      private def runAIAnalysis(prompt: String, agent: Agent): F[String] = {
        val messageRequest = messages.MessageRequest(
          model = "claude-sonnet-4-20250514",
          maxTokens = 64000,
          messages = List(messages.InputMessage.user(prompt)),
          system = Some(agent.prompt.value),
          temperature = Some(0.1), // Low temperature for consistent, analytical responses
          topP = Some(0.95),
          stopSequences = Some(List("---END---", "## XULOSA", "[ANALYSIS_COMPLETE]"))
        )
        for {
          response <- anthropicClient.createMessage(messageRequest)
          analysisResult = response
            .content
            .filter(_.`type` == "text")
            .map(_.text)
            .mkString("\n")
            .replaceAll("#", "")
            .replaceAll("/*", "")
            .replaceAll("`", "")
        } yield analysisResult
      }

      private def saveAnalysisResult(
          id: AnalysisId,
          projectId: ProjectId,
          agentId: AgentId,
          result: String,
          targetUsers: List[AuthedUser.User],
          dateFrom: LocalDate,
          dateTo: LocalDate,
          durationSeconds: Option[Long],
        ): F[Unit] =
        for {
          now <- Calendar[F].currentZonedDateTime
          analysis = Analysis(
            id = id,
            createdAt = now,
            projectId = projectId,
            agentId = agentId,
            response = NonEmptyString.unsafeFrom(result),
            dateFrom = dateFrom,
            dateTo = dateTo,
            durationSeconds = durationSeconds,
          )
          _ <- analysesRepo.create(analysis)
          _ <- analysesRepo.linkUsersToAnalysis(targetUsers.map(_.id), id)
        } yield ()

      private def processAnalysis(id: AnalysisId, filter: AnalysisInput): F[Unit] =
        (for {
          start <- Calendar[F].currentDateTime
          targetUsers <- fetchTargetUsers(filter.userIds)
          project <- fetchProjectInfo(filter.projectId)
          agent <- fetchAgentInfo(filter.agentId)
          _ <- redisClient.del(s"analysis:$id")
          _ <- redisClient.put(s"analysis:$id", AnalysisStatus.GetCommits.entryName, 30.minutes)
          relevantCommits <- fetchRelevantCommits(project, targetUsers, filter.from, filter.to)
          _ <- redisClient.del(s"analysis:$id")
          _ <- redisClient.put(
            s"analysis:$id",
            AnalysisStatus.GetCommitDetails.entryName,
            30.minutes,
          )
          commitDetails <- fetchCommitDetails(project, relevantCommits)
          prompt = buildAnalysisPrompt(commitDetails, targetUsers)
          _ <- redisClient.del(s"analysis:$id")
          _ <- redisClient.put(s"analysis:$id", AnalysisStatus.Analyzing.entryName, 30.minutes)
          analysisResult <- runAIAnalysis(prompt, agent)
          _ <- redisClient.del(s"analysis:$id")
          _ <- redisClient.put(s"analysis:$id", AnalysisStatus.Success.entryName, 10.minutes)
          end <- Calendar[F].currentDateTime
          _ <- saveAnalysisResult(
            id,
            filter.projectId,
            filter.agentId,
            analysisResult,
            targetUsers,
            filter.from,
            filter.to,
            Some(java.time.Duration.between(start, end).getSeconds),
          )
        } yield ()).handleErrorWith { error =>
          val errorMessage = s"Analysis failed: ${error.getMessage}"
          for {
            _ <- redisClient.del(s"analysis:$id")
            _ <- redisClient.put(s"analysis:$id", AnalysisStatus.Failed.entryName, 10.minutes)
            _ <- saveAnalysisResult(
              id,
              filter.projectId,
              filter.agentId,
              errorMessage,
              List.empty,
              filter.from,
              filter.to,
              None,
            )
          } yield ()
        }
    }
}
