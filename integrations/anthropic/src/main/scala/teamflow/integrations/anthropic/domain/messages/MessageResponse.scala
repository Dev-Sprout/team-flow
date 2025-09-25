package teamflow.integrations.anthropic.domain.messages

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class MessageResponse(
    id: String,
    messageType: String,
    role: String,
    content: List[ContentBlock],
    model: String,
    stopReason: Option[String],
    stopSequence: Option[String],
    usage: Usage
)

object MessageResponse {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames.withDefaults
}