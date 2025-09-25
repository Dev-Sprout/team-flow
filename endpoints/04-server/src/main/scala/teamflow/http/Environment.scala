package teamflow.http

import cats.effect.Async
import teamflow.Services
import teamflow.auth.impl.Middlewares
import teamflow.integration.aws.s3.S3Client
import teamflow.support.http4s.HttpServerConfig
import teamflow.support.redis.RedisClient

case class Environment[F[_]: Async](
    config: HttpServerConfig,
    middlewares: Middlewares[F],
    services: Services[F],
    s3Client: S3Client[F],
    redis: RedisClient[F],
  )
