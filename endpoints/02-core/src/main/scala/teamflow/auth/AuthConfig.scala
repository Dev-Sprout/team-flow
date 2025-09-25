package teamflow.auth

import teamflow.domain.JwtAccessTokenKey
import teamflow.domain.TokenExpiration

case class AuthConfig(user: AuthConfig.UserAuthConfig)

object AuthConfig {
  case class UserAuthConfig(
      tokenKey: JwtAccessTokenKey,
      accessTokenExpiration: TokenExpiration,
      refreshTokenExpiration: TokenExpiration,
    )
}
