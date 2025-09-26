package teamflow.services

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import teamflow.domain.PaginatedResponse
import teamflow.domain.ProjectId
import teamflow.domain.Response
import teamflow.domain.projects.Project
import teamflow.domain.projects.ProjectFilter
import teamflow.domain.projects.ProjectInput
import teamflow.effects.Calendar
import teamflow.effects.GenUUID
import teamflow.exception.AError
import teamflow.integrations.github.GithubClient
import teamflow.integrations.github.domain.repository.Repository
import teamflow.repositories.ProjectsRepository
import teamflow.utils.ID

trait ProjectsService[F[_]] {
  def create(input: ProjectInput): F[Response]
  def check(input: ProjectInput): F[Repository]
  def get(filters: ProjectFilter): F[PaginatedResponse[Project]]
  def find(id: ProjectId): F[Option[Project]]
  def update(id: ProjectId, input: ProjectInput): F[Response]
  def delete(id: ProjectId): F[Response]
}

object ProjectsService {
  def make[F[_]: Sync: GenUUID: Calendar](
      projectsRepo: ProjectsRepository[F],
      githubClient: GithubClient[F],
    ): ProjectsService[F] =
    new ProjectsService[F] {
      override def create(input: ProjectInput): F[Response] =
        for {
          id <- ID.make[F, ProjectId]
          repo <- check(input)
          project = Project(
            id = id,
            name = NonEmptyString.from(repo.name).toOption,
            url = NonEmptyString.unsafeFrom(input.url.toString),
          )
          _ <- projectsRepo.create(project)
        } yield Response(id.value, s"Project ${input.url} created")

      override def check(input: ProjectInput): F[Repository] =
        input.url.getPath.stripPrefix("/").split("/").toList match {
          case _ :: repo :: Nil =>
            githubClient.getRepository(NonEmptyString.unsafeFrom(repo))
          case _ => AError.Internal("Invalid GitHub repository URL").raiseError[F, Repository]
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
            Response(id.value, s"Project ${updated.name.getOrElse("unnamed")} updated").pure[F]
          }
          .getOrElse(Response(id.value, s"Project with id $id not found"))

      override def delete(id: ProjectId): F[Response] =
        OptionT(find(id))
          .semiflatMap { existing =>
            projectsRepo.delete(id)
            Response(id.value, s"Project ${existing.name.getOrElse("unnamed")} deleted").pure[F]
          }
          .getOrElse(Response(id.value, s"Project with id $id not found"))
    }
}
