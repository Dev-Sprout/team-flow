package teamflow.integrations.github.domain.repository

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class RepositoryLicense(
    key: String,
    name: String,
    spdxId: Option[String],
    url: Option[String],
    nodeId: String
)

object RepositoryLicense {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames.withDefaults
}