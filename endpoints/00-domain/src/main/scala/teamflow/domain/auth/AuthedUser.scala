package teamflow.domain.auth

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.Email
import teamflow.Username
import teamflow.domain.UserId
import teamflow.domain.enums.Position
import teamflow.domain.enums.Role
import teamflow.syntax.circe._

@JsonCodec
sealed trait AuthedUser {
  val id: UserId
  val createdAt: ZonedDateTime
  val firstName: NonEmptyString
  val lastName: NonEmptyString
  val email: Email
  val username: Username
  val isGithubMember: Boolean
  val role: Role
  val position: Option[Position]
  val avatarUrl: Option[String]
}

object AuthedUser {
  @JsonCodec
  case class User(
      id: UserId,
      createdAt: ZonedDateTime,
      firstName: NonEmptyString,
      lastName: NonEmptyString,
      email: Email,
      username: Username,
      isGithubMember: Boolean,
      role: Role,
      position: Option[Position],
      avatarUrl: Option[String],
    ) extends AuthedUser
}
