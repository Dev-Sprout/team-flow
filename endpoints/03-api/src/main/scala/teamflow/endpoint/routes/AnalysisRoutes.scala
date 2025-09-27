package teamflow.endpoint.routes

import cats.Monad
import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import teamflow.domain.AnalysisId
import teamflow.domain.analyses.AnalysisFilter
import teamflow.domain.analyses.AnalysisInput
import teamflow.domain.auth.AuthedUser
import teamflow.services.AnalysisService
import teamflow.support.http4s.utils.Routes
import teamflow.support.syntax.http4s.http4SyntaxReqOps

final case class AnalysisRoutes[F[_]: Monad: JsonDecoder: MonadThrow](
    analyses: AnalysisService[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/analysis"

  override val public: HttpRoutes[F] = HttpRoutes.empty

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[AnalysisFilter] { filter =>
        analyses.get(filter).flatMap(Ok(_))
      }

    case GET -> Root / "check" / UUIDVar(id) as _ =>
      analyses.check(AnalysisId(id)).flatMap(Ok(_))

    case ar @ POST -> Root / "analyze" as _ =>
      ar.req.decodeR[AnalysisInput] { input =>
        analyses.analyze(input).flatMap(Ok(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      analyses.find(AnalysisId(id)).flatMap(Ok(_))

    case DELETE -> Root / UUIDVar(id) as _ =>
      analyses.delete(AnalysisId(id)).flatMap(Ok(_))
  }
}
