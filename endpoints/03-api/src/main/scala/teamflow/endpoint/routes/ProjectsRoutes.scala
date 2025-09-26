package teamflow.endpoint.routes

import cats.Monad
import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import teamflow.domain.ProjectId
import teamflow.domain.auth.AuthedUser
import teamflow.domain.projects.ProjectFilter
import teamflow.domain.projects.ProjectInput
import teamflow.services.ProjectsService
import teamflow.support.http4s.utils.Routes
import teamflow.support.syntax.http4s.http4SyntaxReqOps

final case class ProjectsRoutes[F[_]: Monad: JsonDecoder: MonadThrow](
    projects: ProjectsService[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/projects"

  override val public: HttpRoutes[F] = HttpRoutes.empty

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[ProjectFilter] { filter =>
        projects.get(filter).flatMap(Ok(_))
      }

    case ar @ POST -> Root / "check" as _ =>
      ar.req.decodeR[ProjectInput] { input =>
        projects.check(input).flatMap(Ok(_))
      }

    case ar @ POST -> Root / "create" as _ =>
      ar.req.decodeR[ProjectInput] { input =>
        projects.create(input).flatMap(Ok(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      projects.find(ProjectId(id)).flatMap(Ok(_))

    case ar @ PUT -> Root / UUIDVar(id) as _ =>
      ar.req.decodeR[ProjectInput] { input =>
        projects.update(ProjectId(id), input).flatMap(Ok(_))
      }

    case DELETE -> Root / UUIDVar(id) as _ =>
      projects.delete(ProjectId(id)).flatMap(Ok(_))
  }
}
