package teamflow.integrations.github.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.refined._
import teamflow.Email

@ConfiguredJsonCodec
case class Committer(
    name: NonEmptyString,
    email: Email,
    date: ZonedDateTime,
  )

object Committer {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}
