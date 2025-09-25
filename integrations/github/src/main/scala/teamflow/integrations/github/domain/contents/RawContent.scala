package teamflow.integrations.github.domain.contents

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class RawContent(
    content: String
)

object RawContent {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}