package teamflow.integrations.github.domain.contents

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class ContentLinks(
    self: String,
    git: String,
    html: String
)

object ContentLinks {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}