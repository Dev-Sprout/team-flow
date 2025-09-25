package teamflow.support.logback

import java.net.URI

import teamflow.support.logback.MonitoringConfig.TelegramConfig

case class MonitoringConfig(telegramAlert: TelegramConfig)

object MonitoringConfig {
  case class TelegramConfig(
      apiUrl: URI,
      chatId: String,
      enabled: Boolean,
    )
}
