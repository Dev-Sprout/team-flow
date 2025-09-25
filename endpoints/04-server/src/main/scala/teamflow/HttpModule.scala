package teamflow

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.kernel.Resource
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import teamflow.domain.auth.AuthedUser
import teamflow.endpoint.routes._
import teamflow.http.Environment
import teamflow.support.http4s.HttpServer
import teamflow.support.http4s.utils.Routes

object HttpModule {
  private def allRoutes[F[_]: Async: JsonDecoder: Logger](
      env: Environment[F]
    ): NonEmptyList[HttpRoutes[F]] =
    NonEmptyList
      .of[Routes[F, AuthedUser]](
        new AuthRoutes[F](env.services.auth),
        new AssetsRoutes[F](env.services.assets),
      )
      .map { r =>
        Router(
          r.path -> (r.public <+> env.middlewares.user(r.`private`))
        )
      }

  def make[F[_]: Async](
      env: Environment[F]
    )(implicit
      logger: Logger[F]
    ): Resource[F, F[ExitCode]] =
    HttpServer.make[F](env.config, implicit wbs => allRoutes[F](env)).map { _ =>
      logger.info(s"Market-Bot http server is started").as(ExitCode.Success)
    }
}
