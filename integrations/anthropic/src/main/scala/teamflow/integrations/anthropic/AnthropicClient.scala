package teamflow.integrations.anthropic

import cats.Applicative
import cats.effect.Sync
import cats.implicits.toFunctorOps
import org.typelevel.log4cats.Logger
import teamflow.integrations.anthropic.domain.messages.{MessageRequest, MessageResponse, InputMessage}
import teamflow.integrations.anthropic.requests.CreateMessage
import teamflow.support.sttp.SttpBackends
import teamflow.support.sttp.SttpClient
import teamflow.support.sttp.SttpClientAuth

trait AnthropicClient[F[_]] {
  def createMessage(request: MessageRequest): F[MessageResponse]
  def sendMessage(message: String, model: Option[String] = None, maxTokens: Option[Int] = None): F[MessageResponse]
  def conversation(messages: List[InputMessage], model: Option[String] = None, maxTokens: Option[Int] = None): F[MessageResponse]
}

object AnthropicClient {
  def make[F[_]: Sync: Logger: SttpBackends.Simple](config: AnthropicConfig): AnthropicClient[F] =
    if (config.enabled)
      new AnthropicClientImpl[F](config)
    else
      new NoOpAnthropicClientImpl[F]

  private class AnthropicClientImpl[F[_]: Sync: SttpBackends.Simple](config: AnthropicConfig)
      extends AnthropicClient[F] {
    private lazy val client: SttpClient.CirceJson[F] =
      SttpClient.circeJson(
        config.apiUrl,
        SttpClientAuth.noAuth,
      )

    override def createMessage(request: MessageRequest): F[MessageResponse] =
      client.request(CreateMessage(request, config.apiKey.value))

    override def sendMessage(message: String, model: Option[String] = None, maxTokens: Option[Int] = None): F[MessageResponse] = {
      val messageRequest = MessageRequest(
        model = model.getOrElse(config.defaultModel.value),
        maxTokens = maxTokens.getOrElse(config.defaultMaxTokens),
        messages = List(InputMessage.user(message))
      )
      createMessage(messageRequest)
    }

    override def conversation(messages: List[InputMessage], model: Option[String] = None, maxTokens: Option[Int] = None): F[MessageResponse] = {
      val messageRequest = MessageRequest(
        model = model.getOrElse(config.defaultModel.value),
        maxTokens = maxTokens.getOrElse(config.defaultMaxTokens),
        messages = messages
      )
      createMessage(messageRequest)
    }
  }

  private class NoOpAnthropicClientImpl[F[_]: Applicative](implicit logger: Logger[F])
      extends AnthropicClient[F] {
    override def createMessage(request: MessageRequest): F[MessageResponse] =
      logger.info(s"Creating message with model [${request.model}]").map(_ => 
        MessageResponse(
          id = "msg_noop",
          messageType = "message",
          role = "assistant",
          content = List.empty,
          model = request.model,
          stopReason = Some("end_turn"),
          stopSequence = None,
          usage = teamflow.integrations.anthropic.domain.messages.Usage(0, 0)
        )
      )

    override def sendMessage(message: String, model: Option[String] = None, maxTokens: Option[Int] = None): F[MessageResponse] =
      logger.info(s"Sending message: [$message] with model [${model.getOrElse("default")}]").map(_ => 
        MessageResponse(
          id = "msg_noop",
          messageType = "message",
          role = "assistant",
          content = List.empty,
          model = model.getOrElse("claude-3-sonnet-20240229"),
          stopReason = Some("end_turn"),
          stopSequence = None,
          usage = teamflow.integrations.anthropic.domain.messages.Usage(0, 0)
        )
      )

    override def conversation(messages: List[InputMessage], model: Option[String] = None, maxTokens: Option[Int] = None): F[MessageResponse] =
      logger.info(s"Having conversation with [${messages.length}] messages").map(_ => 
        MessageResponse(
          id = "msg_noop",
          messageType = "message",
          role = "assistant",
          content = List.empty,
          model = model.getOrElse("claude-3-sonnet-20240229"),
          stopReason = Some("end_turn"),
          stopSequence = None,
          usage = teamflow.integrations.anthropic.domain.messages.Usage(0, 0)
        )
      )
  }
}