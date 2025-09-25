package teamflow.endpoint.routes

import cats.Monad
import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import teamflow.domain.AgentId
import teamflow.domain.agents.AgentFilter
import teamflow.domain.agents.AgentInput
import teamflow.domain.auth.AuthedUser
import teamflow.services.AgentsService
import teamflow.support.http4s.utils.Routes
import teamflow.support.syntax.http4s.http4SyntaxReqOps
import teamflow.syntax.circe._

final case class AgentsRoutes[F[_]: Monad: JsonDecoder: MonadThrow](
    agents: AgentsService[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/agents"

  override val public: HttpRoutes[F] = HttpRoutes.empty

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[AgentFilter] { filter =>
        agents.get(filter).flatMap(Ok(_))
      }

    case ar @ POST -> Root / "create" as _ =>
      ar.req.decodeR[AgentInput] { input =>
        agents.create(input).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      agents.find(AgentId(id)).flatMap(Ok(_))

    case ar @ PUT -> Root / UUIDVar(id) as _ =>
      ar.req.decodeR[AgentInput] { input =>
        agents.update(AgentId(id), input).flatMap(Ok(_))
      }

    case DELETE -> Root / UUIDVar(id) as _ =>
      agents.delete(AgentId(id)).flatMap(Ok(_))
  }
}
