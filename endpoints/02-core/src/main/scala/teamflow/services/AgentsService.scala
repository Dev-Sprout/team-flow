package teamflow.services

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import teamflow.domain.AgentId
import teamflow.domain.PaginatedResponse
import teamflow.domain.Response
import teamflow.domain.agents.Agent
import teamflow.domain.agents.AgentFilter
import teamflow.domain.agents.AgentInput
import teamflow.effects.Calendar
import teamflow.effects.GenUUID
import teamflow.repositories.AgentsRepository
import teamflow.utils.ID

trait AgentsService[F[_]] {
  def create(input: AgentInput): F[Response]
  def get(filters: AgentFilter): F[PaginatedResponse[Agent]]
  def find(id: AgentId): F[Option[Agent]]
  def update(id: AgentId, input: AgentInput): F[Response]
  def delete(id: AgentId): F[Response]
}

object AgentsService {
  def make[F[_]: Sync: GenUUID: Calendar](
      agentsRepo: AgentsRepository[F]
    ): AgentsService[F] =
    new AgentsService[F] {
      override def create(input: AgentInput): F[Response] =
        for {
          id <- ID.make[F, AgentId]
          now <- Calendar[F].currentZonedDateTime
          agent = Agent(
            id = id,
            createdAt = now,
            name = input.name,
            prompt = input.prompt,
            description = input.description,
          )
          _ <- agentsRepo.create(agent)
        } yield Response(id.value, s"Agent ${agent.name.value} created")

      override def get(filters: AgentFilter): F[PaginatedResponse[Agent]] =
        agentsRepo.get(filters)

      override def find(id: AgentId): F[Option[Agent]] =
        agentsRepo.findById(id)

      override def update(id: AgentId, input: AgentInput): F[Response] =
        OptionT(find(id))
          .semiflatMap { existing =>
            val updated = existing.copy(
              name = input.name,
              prompt = input.prompt,
              description = input.description,
            )
            agentsRepo.update(updated)
            Response(id.value, s"Agent ${updated.name.value} updated").pure[F]
          }
          .getOrElse(Response(id.value, s"Agent with id $id not found"))

      override def delete(id: AgentId): F[Response] =
        OptionT(find(id))
          .semiflatMap { existing =>
            agentsRepo.delete(id)
            Response(id.value, s"Agent ${existing.name.value} deleted").pure[F]
          }
          .getOrElse(Response(id.value, s"Agent with id $id not found"))
    }
}
