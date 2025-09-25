package teamflow.integrations.anthropic.domain.messages

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class InputMessage(
    role: String,
    content: String
)

object InputMessage {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames

  def user(content: String): InputMessage = InputMessage("user", content)
  def assistant(content: String): InputMessage = InputMessage("assistant", content)
}