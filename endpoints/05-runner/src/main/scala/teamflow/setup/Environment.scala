package teamflow.setup

import cats.MonadThrow
import cats.effect.Async
import cats.effect.Resource
import cats.effect.std.Console
import cats.effect.std.Random
import cats.implicits.catsSyntaxOptionId
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger
import pureconfig.generic.auto.exportReader
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import teamflow.JobsEnvironment
import teamflow.Repositories
import teamflow.Services
import teamflow.auth.impl.Middlewares
import teamflow.http.{ Environment => ServerEnvironment }
import teamflow.integration.aws.s3.S3Client
import teamflow.integrations.github.GithubClient
import teamflow.support.database.Migrations
import teamflow.support.redis.RedisClient
import teamflow.support.skunk.SkunkSession
import teamflow.utils.ConfigLoader

case class Environment[F[_]: Async: MonadThrow: Logger](
    config: Config,
    repositories: Repositories[F],
    services: Services[F],
    middlewares: Middlewares[F],
    s3Client: S3Client[F],
    redis: RedisClient[F],
    githubClient: GithubClient[F],
  ) {
  lazy val jobsEnabled: Boolean = config.jobs.enabled
  lazy val toServer: ServerEnvironment[F] =
    ServerEnvironment(
      middlewares = middlewares,
      services = services,
      config = config.httpServer,
      s3Client = s3Client,
      redis = redis,
    )
  lazy val toJobs: JobsEnvironment[F] =
    JobsEnvironment(
      repos = JobsEnvironment.Repositories()
    )
}

object Environment {
  def make[F[_]: Async: Console: Logger]: Resource[F, Environment[F]] =
    for {
      config <- Resource.eval(ConfigLoader.load[F, Config])
      _ <- Resource.eval(Migrations.run[F](config.migrations))

      redis <- Redis[F].utf8(config.redis.uri.toString).map(RedisClient[F](_, config.redis.prefix))
      repositories <- SkunkSession.make[F](config.database).map { implicit session =>
        Repositories.make[F]
      }

      githubClient <- HttpClientFs2Backend.resource[F]().map { implicit backend =>
        GithubClient.make[F](NonEmptyString.unsafeFrom("test").some)
      }

      implicit0(random: Random[F]) <- Resource.eval(Random.scalaUtilRandom)
      s3Client <- S3Client.resource(config.s3)
      services = Services
        .make[F](
          config.auth,
          repositories,
          redis,
          s3Client,
          githubClient,
        )
      middleware = Middlewares.make[F](config.auth, redis)
    } yield Environment[F](
      config,
      repositories,
      services,
      middleware,
      s3Client,
      redis,
      githubClient,
    )
}
