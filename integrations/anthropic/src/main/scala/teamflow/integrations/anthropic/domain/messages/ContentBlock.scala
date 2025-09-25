package teamflow.integrations.anthropic.domain.messages

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class ContentBlock(
    blockType: String,
    text: String
)

object ContentBlock {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}