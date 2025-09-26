package teamflow.endpoint.routes

import cats.Monad
import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import teamflow.domain.UserId
import teamflow.domain.auth.AuthedUser
import teamflow.domain.users.UserFilter
import teamflow.domain.users.UserInput
import teamflow.services.UsersService
import teamflow.support.http4s.utils.Routes
import teamflow.support.syntax.http4s.http4SyntaxReqOps
import teamflow.syntax.refined._

final case class UsersRoutes[F[_]: Monad: JsonDecoder: MonadThrow](
    users: UsersService[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/users"

  override val public: HttpRoutes[F] = HttpRoutes.empty

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[UserFilter] { filter =>
        users.get(filter).flatMap(Ok(_))
      }

    case GET -> Root / "check" / username as _ =>
      users.check(username).flatMap(Ok(_))

    case ar @ POST -> Root / "create" as _ =>
      ar.req.decodeR[UserInput] { input =>
        users.create(input).flatMap(Ok(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      users.find(UserId(id)).flatMap(Ok(_))

    case ar @ PUT -> Root / UUIDVar(id) as _ =>
      ar.req.decodeR[UserInput] { input =>
        users.update(UserId(id), input).flatMap(Ok(_))
      }

    case DELETE -> Root / UUIDVar(id) as _ =>
      users.delete(UserId(id)).flatMap(Ok(_))
  }
}
