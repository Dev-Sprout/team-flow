package teamflow.integrations.github.domain.commits

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.extras.ConfiguredJsonCodec
import teamflow.Email

import java.time.ZonedDateTime

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
