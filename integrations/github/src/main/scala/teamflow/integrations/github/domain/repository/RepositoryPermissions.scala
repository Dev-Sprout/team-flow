package teamflow.integrations.github.domain.repository

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class RepositoryPermissions(
    admin: Boolean,
    maintain: Option[Boolean],
    push: Boolean,
    triage: Option[Boolean],
    pull: Boolean
)

object RepositoryPermissions {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames.withDefaults
}