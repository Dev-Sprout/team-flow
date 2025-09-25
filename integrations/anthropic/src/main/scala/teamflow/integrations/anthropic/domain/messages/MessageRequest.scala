package teamflow.integrations.anthropic.domain.messages

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class MessageRequest(
    model: String,
    maxTokens: Int,
    messages: List[InputMessage],
    system: Option[String] = None,
    temperature: Option[Double] = None,
    topP: Option[Double] = None,
    topK: Option[Int] = None,
    stopSequences: Option[List[String]] = None,
    stream: Option[Boolean] = None
)

object MessageRequest {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames.withDefaults
}