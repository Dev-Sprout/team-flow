package teamflow.services

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import teamflow.domain.PaginatedResponse
import teamflow.domain.ProjectId
import teamflow.domain.Response
import teamflow.domain.agents.Agent
import teamflow.domain.auth.AuthedUser
import teamflow.domain.projects.Project
import teamflow.domain.projects.ProjectAnalyze
import teamflow.domain.projects.ProjectFilter
import teamflow.domain.projects.ProjectInput
import teamflow.domain.users.UserFilter
import teamflow.effects.Calendar
import teamflow.effects.GenUUID
import teamflow.exception.AError
import teamflow.integrations.anthropic.AnthropicClient
import teamflow.integrations.github.GithubClient
import teamflow.integrations.github.domain.repository.Repository
import teamflow.repositories.AgentsRepository
import teamflow.repositories.ProjectsRepository
import teamflow.repositories.UsersRepository
import teamflow.utils.ID

trait ProjectsService[F[_]] {
  def create(input: ProjectInput): F[Response]
  def check(input: ProjectInput): F[Repository]
  def analyze(filter: ProjectAnalyze): F[String]
  def get(filters: ProjectFilter): F[PaginatedResponse[Project]]
  def find(id: ProjectId): F[Option[Project]]
  def update(id: ProjectId, input: ProjectInput): F[Response]
  def delete(id: ProjectId): F[Response]
}

object ProjectsService {
  def make[F[_]: Sync: GenUUID: Calendar](
      projectsRepo: ProjectsRepository[F],
      agentsRepo: AgentsRepository[F],
      usersRepo: UsersRepository[F],
      githubClient: GithubClient[F],
      anthropicClient: AnthropicClient[F],
    ): ProjectsService[F] =
    new ProjectsService[F] {
      override def create(input: ProjectInput): F[Response] =
        for {
          id <- ID.make[F, ProjectId]
          repo <- check(input)
          project = Project(
            id = id,
            name = NonEmptyString.unsafeFrom(repo.name),
            url = NonEmptyString.unsafeFrom(input.url.toString),
          )
          _ <- projectsRepo.create(project)
        } yield Response(id.value, s"Project ${input.url} created")

      override def check(input: ProjectInput): F[Repository] =
        input.url.getPath.stripPrefix("/").split("/").toList match {
          case _ :: repo :: Nil =>
            githubClient.getRepository(NonEmptyString.unsafeFrom(repo))
          case _ => Sync[F].raiseError[Repository](AError.Internal("Invalid GitHub repository URL"))
        }

      override def analyze(filter: ProjectAnalyze): F[String] =
        for {
          // Faqat kerakli userlarni olish (filter.userIds dan)
          allUsers <- usersRepo.get(UserFilter()).map(_.data.map(u => u.id -> u).toMap)
          targetUsers = filter.userIds.toList.flatMap(userId => allUsers.get(userId))

          project <- OptionT(projectsRepo.findById(filter.projectId))
            .getOrElseF(
              Sync[F].raiseError[Project](
                AError.BadRequest(s"Project with id ${filter.projectId} not found")
              )
            )

          agent <- OptionT(agentsRepo.findById(filter.agentId))
            .getOrElseF(
              Sync[F].raiseError[Agent](
                AError.BadRequest(s"Agent with id ${filter.agentId} not found")
              )
            )

          // Commitlarni olish
          commits <- githubClient.getCommits(project.name)

          // Debug: commit count va target users
          _ <- Sync[F].delay(
            println(s"DEBUG: Found ${commits.length} commits for project ${project.name}")
          )
          _ <- Sync[F].delay(
            println(
              s"DEBUG: Target users: ${targetUsers.map(u => s"${u.firstName} ${u.lastName} <${u.email}>").mkString(", ")}"
            )
          )

          // Faqat target userlar tomonidan qilingan commitlarni filter qilish
          targetUserEmails = targetUsers.map(_.email).toSet
          _ <- Sync[F].delay(println(s"DEBUG: Target emails: ${targetUserEmails.mkString(", ")}"))

          filteredCommits = commits.filter { commit =>
            val authorEmail = commit.commit.author.email
            val committerEmail = commit.commit.committer.email
            val matches =
              targetUserEmails.contains(authorEmail) || targetUserEmails.contains(committerEmail)
            if (matches)
              println(
                s"DEBUG: Commit ${commit.sha.take(8)} matches - author: $authorEmail, committer: $committerEmail"
              )
            matches
          }
          _ <- Sync[F].delay(println(s"DEBUG: Filtered to ${filteredCommits.length} commits"))

          // Agar commit yo'q bo'lsa, xabar berish
          _ <-
            if (filteredCommits.isEmpty)
              Sync[F].delay(println("DEBUG: No matching commits found. This might be because:")) >>
                Sync[F].delay(println("1. GitHub integration is disabled (NoOp mode)")) >>
                Sync[F].delay(println("2. No commits match the target user emails")) >>
                Sync[F].delay(println("3. Repository has no commits"))
            else
              Sync[F].unit

          // Faqat birinchi 5 ta commit uchun details olish (performance uchun)
          limitedCommits = filteredCommits.take(5)
          _ <- Sync[F].delay(
            println(
              s"DEBUG: Processing ${limitedCommits.length} commits (limited from ${filteredCommits.length})"
            )
          )

          // Commit details olish
          commitDetails <- limitedCommits.traverse(c =>
            githubClient.getCommitDetails(project.name, c.sha)
          )

          // Har bir commit uchun file contentlarni olish (tezlik uchun cheklangan)
          commitWithContents <- commitDetails.traverse { commit =>
            val limitedFiles = commit.files.take(5).filter(_.changes <= 200)
            for {
              // Faqat 5 ta faylgacha va kichik fayllar uchun content olish
              fileContents <- limitedFiles.traverse { file =>
                if (
                    (file.filename.endsWith(".scala") || file.filename.endsWith(".java") ||
                    file.filename.endsWith(".js") || file.filename.endsWith(".ts") ||
                    file.filename.endsWith(".py")) && file.additions + file.deletions <= 100
                )
                  // Faqat kichik o'zgarishlar uchun full content olish
                  s"File: ${file.filename}\n${file.patch.getOrElse("")}\nChanges: +${file.additions} -${file.deletions}"
                    .pure[F]
                else
                  s"File: ${file.filename}\n${file.patch.getOrElse("")}\nChanges: +${file.additions} -${file.deletions}"
                    .pure[F]
              }
            } yield (commit, fileContents)
          }

          // Analysis uchun prompt yaratish
          analysisPrompt = buildAnalysisPrompt(commitWithContents, targetUsers, agent)

          // Anthropic AI ga yuborish
          response <- anthropicClient.sendMessage(
            message = analysisPrompt,
            maxTokens = Some(4000),
          )

          // Response dan text extract qilish
          analysisResult = response
            .content
            .filter(_.`type` == "text")
            .map(_.text)
            .mkString("\n")

        } yield analysisResult

      private def buildAnalysisPrompt(
          commitWithContents: List[
            (teamflow.integrations.github.domain.commits.CommitDetails, List[String])
          ],
          targetUsers: List[AuthedUser.User],
          agent: Agent,
        ): String = {
        val userInfo =
          targetUsers.map(u => s"- ${u.firstName} ${u.lastName} (${u.email})").mkString("\n")
        val commitInfo = commitWithContents
          .map {
            case (commit, contents) =>
              s"""
          |Commit SHA: ${commit.sha}
          |Author: ${commit.commit.author.name} <${commit.commit.author.email}>
          |Date: ${commit.commit.author.date}
          |Message: ${commit.commit.message}
          |
          |Changed Files and Content:
          |${contents.mkString("\n\n")}
          |
          |Stats: +${commit.stats.additions} -${commit.stats.deletions} (${commit.stats.total} changes)
          |""".stripMargin
          }
          .mkString("\n" + "=" * 80 + "\n")

        s"""
        |${agent.prompt.value}
        |
        |Please analyze the following code commits made by these developers:
        |
        |Target Developers:
        |$userInfo
        |
        |Code Commits to Analyze:
        |$commitInfo
        |
        |Please provide a comprehensive analysis including:
        |1. Code quality assessment
        |2. Coding patterns and practices used
        |3. Potential issues or improvements
        |4. Summary of changes and their impact
        |5. Developer-specific insights and recommendations
        |
        |Focus on constructive feedback and actionable insights.
        |""".stripMargin
      }

      override def get(filters: ProjectFilter): F[PaginatedResponse[Project]] =
        projectsRepo.get(filters)

      override def find(id: ProjectId): F[Option[Project]] =
        projectsRepo.findById(id)

      override def update(id: ProjectId, input: ProjectInput): F[Response] =
        OptionT(find(id))
          .semiflatMap { existing =>
            val updated = existing.copy(
              url = NonEmptyString.unsafeFrom(input.url.toString)
            )
            projectsRepo.update(updated)
            Response(id.value, s"Project ${updated.name} updated").pure[F]
          }
          .getOrElse(Response(id.value, s"Project with id $id not found"))

      override def delete(id: ProjectId): F[Response] =
        OptionT(find(id))
          .semiflatMap { existing =>
            projectsRepo.delete(id)
            Response(id.value, s"Project ${existing.name} deleted").pure[F]
          }
          .getOrElse(Response(id.value, s"Project with id $id not found"))
    }
}
