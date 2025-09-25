package teamflow.integrations.telegram.domain

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class MenuButtonWebApp(
                             `type`: String,
                             text: String,
                             webApp: WebAppInfo
                           )

object MenuButtonWebApp {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}