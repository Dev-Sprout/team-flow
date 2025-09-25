package teamflow.domain.auth

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.Username

@JsonCodec
case class Credentials(username: Username, password: NonEmptyString)
