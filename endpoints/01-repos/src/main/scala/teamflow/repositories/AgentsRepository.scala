package teamflow.repositories

import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import teamflow.domain.AgentId
import teamflow.domain.PaginatedResponse
import teamflow.domain.agents.Agent
import teamflow.domain.agents.AgentFilter
import teamflow.repositories.sql.AgentsSql
import teamflow.support.skunk.syntax.all._

trait AgentsRepository[F[_]] {
  def get(filter: AgentFilter): F[PaginatedResponse[Agent]]
  def findById(id: AgentId): F[Option[Agent]]
  def findByIds(ids: List[AgentId]): F[Map[AgentId, Agent]]
  def create(agent: Agent): F[Unit]
  def update(agent: Agent): F[Unit]
  def delete(id: AgentId): F[Unit]
}

object AgentsRepository {
  def make[F[_]: fs2.Compiler.Target](
      implicit
      session: Resource[F, Session[F]]
    ): AgentsRepository[F] = new AgentsRepository[F] {
    override def get(filter: AgentFilter): F[PaginatedResponse[Agent]] = {
      val af = AgentsSql.getByFilter(filter).paginateOpt(filter.limit, filter.page)
      for {
        agents <- af.fragment.query(AgentsSql.codec *: int8).queryList(af.argument)
        list = agents.map(_.head)
        count = agents.headOption.fold(0L)(_.tail.head)
      } yield PaginatedResponse(list, count)
    }

    override def findById(id: AgentId): F[Option[Agent]] =
      AgentsSql.findById.queryOption(id)

    override def findByIds(ids: List[AgentId]): F[Map[AgentId, Agent]] =
      if (ids.isEmpty) Map.empty[AgentId, Agent].pure[F]
      else
        AgentsSql
          .findByIds(ids)
          .map(a => a.id -> a)
          .queryList(ids)
          .map(_.toMap)

    override def create(agent: Agent): F[Unit] =
      AgentsSql.insert.execute(agent)

    override def update(agent: Agent): F[Unit] =
      AgentsSql.update.execute(agent)

    override def delete(id: AgentId): F[Unit] =
      AgentsSql.delete.execute(id)
  }
}
