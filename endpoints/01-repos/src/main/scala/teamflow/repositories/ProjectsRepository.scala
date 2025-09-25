package teamflow.repositories

import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import skunk.codec.all.int8
import teamflow.domain.PaginatedResponse
import teamflow.domain.ProjectId
import teamflow.domain.projects.Project
import teamflow.domain.projects.ProjectFilter
import teamflow.repositories.sql.ProjectsSql
import teamflow.support.skunk.syntax.all._

trait ProjectsRepository[F[_]] {
  def get(filter: ProjectFilter): F[PaginatedResponse[Project]]
  def findById(id: ProjectId): F[Option[Project]]
  def create(project: Project): F[Unit]
  def update(project: Project): F[Unit]
  def delete(id: ProjectId): F[Unit]
}

object ProjectsRepository {
  def make[F[_]: fs2.Compiler.Target](
      implicit
      session: Resource[F, Session[F]]
    ): ProjectsRepository[F] = new ProjectsRepository[F] {
    override def get(filter: ProjectFilter): F[PaginatedResponse[Project]] = {
      val af = ProjectsSql.getByFilter(filter).paginateOpt(filter.limit, filter.page)
      for {
        projects <- af.fragment.query(ProjectsSql.codec *: int8).queryList(af.argument)
        list = projects.map(_.head)
        count = projects.headOption.fold(0L)(_.tail.head)
      } yield PaginatedResponse(list, count)
    }

    override def findById(id: ProjectId): F[Option[Project]] =
      ProjectsSql.findById.queryOption(id)

    override def create(project: Project): F[Unit] =
      ProjectsSql.insert.execute(project)

    override def update(project: Project): F[Unit] =
      ProjectsSql.update.execute(project)

    override def delete(id: ProjectId): F[Unit] =
      ProjectsSql.delete.execute(id)
  }
}
