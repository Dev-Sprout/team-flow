package teamflow.auth.impl

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.auth.jwt.JwtToken
import io.circe.Decoder
import org.http4s.server
import pdi.jwt.JwtAlgorithm
import teamflow.auth.AuthConfig.UserAuthConfig
import teamflow.auth.utils.AuthMiddleware
import teamflow.domain.auth.AuthedUser
import teamflow.support.redis.RedisClient
import teamflow.syntax.all.circeSyntaxDecoderOps
import teamflow.syntax.refined.commonSyntaxAutoUnwrapV

object LiveMiddleware {
  def make[F[_]: Sync, U <: AuthedUser: Decoder](
      jwtConfig: UserAuthConfig,
      redis: RedisClient[F],
    ): server.AuthMiddleware[F, U] = {
    val userJwtAuth = JwtAuth.hmac(jwtConfig.tokenKey.secret, JwtAlgorithm.HS256)
    def findUser(token: String): F[Option[U]] =
      OptionT(redis.get(token))
        .semiflatMap(_.decodeAsF[F, U])
        .value

    def destroySession(token: JwtToken): F[Unit] =
      OptionT(findUser(AuthMiddleware.ACCESS_TOKEN_PREFIX + token.value))
        .semiflatMap(user =>
          redis.del(AuthMiddleware.ACCESS_TOKEN_PREFIX + token.value, user.username)
        )
        .value
        .void

    AuthMiddleware[F, U](
      userJwtAuth,
      findUser,
      destroySession,
    )
  }
}
