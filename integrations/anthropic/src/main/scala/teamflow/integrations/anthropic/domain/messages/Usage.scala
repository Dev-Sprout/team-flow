package teamflow.integrations.anthropic.domain.messages

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class Usage(
    inputTokens: Int,
    outputTokens: Int
)

object Usage {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}