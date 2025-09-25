package teamflow.integrations.github.domain.commits

import io.circe.generic.extras.ConfiguredJsonCodec

import java.time.ZonedDateTime

@ConfiguredJsonCodec
case class Verification(
    verified: Boolean,
    reason: String,
    signature: Option[String],
    payload: Option[String],
    verifiedAt: Option[ZonedDateTime],
  )

object Verification {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}
