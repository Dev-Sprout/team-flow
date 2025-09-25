package teamflow.services

import cats.MonadThrow
import cats.effect.Sync
import cats.implicits._
import teamflow.domain.AgentId
import teamflow.domain.AssetId
import teamflow.domain.PaginatedResponse
import teamflow.domain.Response
import teamflow.domain.UserId
import teamflow.domain.agents.Agent
import teamflow.domain.agents.AgentFilter
import teamflow.domain.agents.AgentInput
import teamflow.domain.auth.AuthedUser
import teamflow.domain.users.UserFilter
import teamflow.domain.users.UserInput
import teamflow.effects.Calendar
import teamflow.effects.FileLoader
import teamflow.effects.GenUUID
import teamflow.syntax.refined._
import teamflow.utils.ID

trait AgentsService[F[_]] {
  def create(input: AgentInput): F[Response]
  def get(filters: AgentFilter): F[PaginatedResponse[Agent]]
  def find(id: AgentId): F[Option[Agent]]
  def update(id: AgentId, input: AgentInput): F[Response]
  def delete(id: AgentId): F[Response]
}

object AgentsService {
  def make[F[_]: Sync: FileLoader: GenUUID: Calendar](
    ): AgentsService[F] =
    new AgentsService[F] {
      override def create(input: AgentInput): F[Response] = ???

      override def get(filters: AgentFilter): F[PaginatedResponse[Agent]] = ???

      override def find(id: AgentId): F[Option[Agent]] = ???

      override def update(id: AgentId, input: AgentInput): F[Response] = ???

      override def delete(id: AgentId): F[Response] = ???
    }
}
