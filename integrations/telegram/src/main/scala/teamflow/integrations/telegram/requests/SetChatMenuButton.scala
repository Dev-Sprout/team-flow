package teamflow.integrations.telegram.requests

import teamflow.integrations.telegram.domain.MenuButtonWebApp
import teamflow.support.sttp.SttpRequest
import io.circe.Json
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import sttp.model.Method

@ConfiguredJsonCodec
case class SetChatMenuButton(
                              chatId: Long,
                              menuButton: Option[MenuButtonWebApp],
                            )

object SetChatMenuButton {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit def sttpRequest: SttpRequest[SetChatMenuButton, Json] =
    new SttpRequest[SetChatMenuButton, Json] {
      val method: Method = Method.POST

      override def path: Path = r => s"setChatMenuButton"

      def body: Body = jsonBody
    }
}
