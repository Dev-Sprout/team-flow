package teamflow.integrations.github.domain.commits

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.extras.ConfiguredJsonCodec
import teamflow.Email

import java.time.ZonedDateTime

@ConfiguredJsonCodec
case class Author(
    name: NonEmptyString,
    email: Email,
    date: ZonedDateTime,
  )

object Author {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}
