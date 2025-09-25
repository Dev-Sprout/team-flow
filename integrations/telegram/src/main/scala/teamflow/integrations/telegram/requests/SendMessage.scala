package teamflow.integrations.telegram.requests

import io.circe.Json
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec
import sttp.model.Method

import teamflow.integrations.telegram.domain.MessageEntity
import teamflow.integrations.telegram.domain.ReplyMarkup
import teamflow.support.sttp.SttpRequest

@ConfiguredJsonCodec
case class SendMessage(
    chatId: Long,
    text: String,
    replyMarkup: Option[ReplyMarkup],
    entities: Option[List[MessageEntity]],
  )

object SendMessage {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit def sttpRequest: SttpRequest[SendMessage, Json] =
    new SttpRequest[SendMessage, Json] {
      val method: Method = Method.POST
      override def path: Path = r => s"sendMessage"
      def body: Body = jsonBody
    }
}
