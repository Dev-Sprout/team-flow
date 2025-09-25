package teamflow.domain.users

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.Email
import teamflow.Username
import teamflow.domain.enums.Position
import teamflow.domain.enums.Role
import teamflow.syntax.circe._

@JsonCodec
case class UserInput(
    firstName: NonEmptyString,
    lastName: NonEmptyString,
    email: Email,
    username: Username,
    role: Role,
    position: Option[Position],
  )
