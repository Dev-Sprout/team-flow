package teamflow.auth.impl

import cats.data.EitherT
import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import dev.profunktor.auth.jwt.JwtToken
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.Request
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtAlgorithm
import teamflow.Phone
import teamflow.Username
import teamflow.auth.AuthConfig.UserAuthConfig
import teamflow.auth.utils.AuthMiddleware
import teamflow.auth.utils.JwtExpire
import teamflow.auth.utils.Tokens
import teamflow.domain.auth.AccessCredentials
import teamflow.domain.auth.AuthTokens
import teamflow.domain.auth.AuthedUser
import teamflow.domain.auth.Credentials
import teamflow.exception.AError.AuthError
import teamflow.exception.AError.AuthError.NoSuchUser
import teamflow.exception.AError.AuthError.PasswordDoesNotMatch
import teamflow.support.redis.RedisClient
import teamflow.syntax.all.circeSyntaxDecoderOps
import teamflow.syntax.refined.commonSyntaxAutoUnwrapV
import tsec.passwordhashers.jca.SCrypt

trait Auth[F[_], A] {
  def login(credentials: Credentials): F[AuthTokens]
  def destroySession(request: Request[F], username: Username): F[Unit]
  def refresh(request: Request[F]): F[AuthTokens]
}

object Auth {
  def make[F[_]: Sync, U <: AuthedUser: Encoder: Decoder](
      config: UserAuthConfig,
      findUser: Username => F[Option[AccessCredentials[U]]],
      redis: RedisClient[F],
    )(implicit
      logger: Logger[F]
    ): Auth[F, U] =
    new Auth[F, U] {
      val tokens: Tokens[F] =
        Tokens.make[F](JwtExpire[F], config)
      val jwtAuth: JwtSymmetricAuth = JwtAuth.hmac(config.tokenKey.secret, JwtAlgorithm.HS256)

      override def login(credentials: Credentials): F[AuthTokens] =
        findUser(credentials.username).flatMap {
          case None =>
            NoSuchUser("User Not Found").raiseError[F, AuthTokens]
          case Some(person) if !SCrypt.checkpwUnsafe(credentials.password, person.password) =>
            PasswordDoesNotMatch("Password does not match").raiseError[F, AuthTokens]
          case Some(person) =>
            OptionT(redis.get(credentials.username))
              .cataF(
                createNewToken(person.data),
                json =>
                  for {
                    tokens <- json.decodeAsF[F, AuthTokens]
                    validTokens <- EitherT(
                      AuthMiddleware
                        .validateJwtToken[F](
                          JwtToken(tokens.accessToken),
                          jwtAuth,
                          _ =>
                            redis.del(tokens.accessToken, tokens.refreshToken, credentials.username),
                        )
                    ).foldF(
                      error =>
                        logger.info(s"Tokens recreated reason of that: $error") *>
                          createNewToken(person.data),
                      _ => tokens.pure[F],
                    )
                  } yield validTokens,
              )
        }

      override def refresh(request: Request[F]): F[AuthTokens] = {
        val task = for {
          refreshToken <- EitherT(
            AuthMiddleware
              .getAndValidateJwtToken[F](
                jwtAuth,
                token =>
                  for {
                    _ <- OptionT(redis.get(AuthMiddleware.REFRESH_TOKEN_PREFIX + token))
                      .semiflatMap(_.decodeAsF[F, U])
                      .semiflatMap(person => redis.del(person.username))
                      .value
                    _ <- redis.del(AuthMiddleware.REFRESH_TOKEN_PREFIX + token.value)
                  } yield {},
              )
              .apply(request)
          )
          person <- EitherT
            .fromOptionF(
              redis.get(AuthMiddleware.REFRESH_TOKEN_PREFIX + refreshToken.value),
              "Refresh token expired",
            )
            .semiflatMap(_.decodeAsF[F, U])
          _ <- EitherT.right[String](clearOldTokens(person.username))
          tokens <- EitherT.right[String](createNewToken(person))
        } yield tokens
        task.leftMap(AuthError.InvalidToken.apply).rethrowT
      }

      override def destroySession(request: Request[F], username: Username): F[Unit] =
        AuthHeaders
          .getBearerToken(request)
          .traverse_(token => redis.del(AuthMiddleware.ACCESS_TOKEN_PREFIX + token.value, username))

      private def createNewToken(person: U): F[AuthTokens] =
        for {
          tokens <- tokens.createToken[U](person)
          accessToken = AuthMiddleware.ACCESS_TOKEN_PREFIX + tokens.accessToken
          refreshToken = AuthMiddleware.REFRESH_TOKEN_PREFIX + tokens.refreshToken
          _ <- redis.put(accessToken, person, config.accessTokenExpiration.value)
          _ <- redis.put(refreshToken, person, config.refreshTokenExpiration.value)
          _ <- redis.put(person.username, tokens, config.refreshTokenExpiration.value)
        } yield tokens

      private def clearOldTokens(username: Username): F[Unit] =
        OptionT(redis.get(username))
          .semiflatMap(_.decodeAsF[F, AuthTokens])
          .semiflatMap(tokens =>
            redis.del(
              s"${AuthMiddleware.REFRESH_TOKEN_PREFIX}${tokens.refreshToken}",
              s"${AuthMiddleware.ACCESS_TOKEN_PREFIX}${tokens.accessToken}",
            )
          )
          .value
          .void
    }
}
