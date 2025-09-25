package teamflow.integrations.github.domain.commits

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class Parent(
    sha: String,
    url: String,
    htmlUrl: String,
  )

object Parent {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}
