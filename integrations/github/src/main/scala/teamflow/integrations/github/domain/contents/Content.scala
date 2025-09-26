package teamflow.integrations.github.domain.contents

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class Content(
    name: String,
    path: String,
    sha: String,
    size: Long,
    url: String,
    htmlUrl: String,
    gitUrl: String,
    downloadUrl: Option[String],
    `type`: String,
    content: Option[String],
    encoding: Option[String],
    _links: ContentLinks
)

object Content {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames.withDefaults
}