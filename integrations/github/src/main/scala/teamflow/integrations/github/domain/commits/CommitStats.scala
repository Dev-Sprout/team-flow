package teamflow.integrations.github.domain.commits

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class CommitStats(
    total: Int,
    additions: Int,
    deletions: Int
)

object CommitStats {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}