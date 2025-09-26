package teamflow.domain.users

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.domain.enums.Position
import teamflow.domain.enums.Role
import teamflow.syntax.circe._

@JsonCodec
case class UserFilter(
    fullName: Option[NonEmptyString] = None,
    role: Option[Role] = None,
    position: Option[Position] = None,
    isGithubMember: Option[Boolean] = None,
    limit: Option[PosInt] = None,
    page: Option[PosInt] = None,
  )
