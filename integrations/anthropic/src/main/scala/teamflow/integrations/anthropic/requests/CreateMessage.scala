package teamflow.integrations.anthropic.requests

import sttp.model.Method
import teamflow.integrations.anthropic.domain.messages.{MessageRequest, MessageResponse}
import teamflow.support.sttp.SttpRequest
import io.circe.generic.extras.auto._

case class CreateMessage(
    request: MessageRequest,
    apiKey: String
)

object CreateMessage {
  implicit val sttpRequest: SttpRequest[CreateMessage, MessageResponse] =
    new SttpRequest[CreateMessage, MessageResponse] {
      val method: Method = Method.POST
      override def path: Path = _ => "v1/messages"
      override def headers: Headers = r => Map(
        "x-api-key" -> r.apiKey,
        "anthropic-version" -> "2023-06-01",
        "Content-Type" -> "application/json"
      )

      def body: Body = jsonBodyFrom(_.request)
    }
}