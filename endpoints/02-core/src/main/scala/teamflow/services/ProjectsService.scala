package teamflow.services

import cats.effect.Sync
import teamflow.domain.PaginatedResponse
import teamflow.domain.ProjectId
import teamflow.domain.Response
import teamflow.domain.projects.Project
import teamflow.domain.projects.ProjectFilter
import teamflow.domain.projects.ProjectInput
import teamflow.effects.Calendar
import teamflow.effects.FileLoader
import teamflow.effects.GenUUID

trait ProjectsService[F[_]] {
  def create(input: ProjectInput): F[Response]
  def get(filters: ProjectFilter): F[PaginatedResponse[Project]]
  def find(id: ProjectId): F[Option[Project]]
  def update(id: ProjectId, input: ProjectInput): F[Response]
  def delete(id: ProjectId): F[Response]
}

object ProjectsService {
  def make[F[_]: Sync: FileLoader: GenUUID: Calendar: Lambda[M[_] => fs2.Compiler[M, M]]](
    ): ProjectsService[F] =
    new ProjectsService[F] {
      override def create(input: ProjectInput): F[Response] = ???

      override def get(filters: ProjectFilter): F[PaginatedResponse[Project]] = ???

      override def find(id: ProjectId): F[Option[Project]] = ???

      override def update(id: ProjectId, input: ProjectInput): F[Response] = ???

      override def delete(id: ProjectId): F[Response] = ???
    }
}
