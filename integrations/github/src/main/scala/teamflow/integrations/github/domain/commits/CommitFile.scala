package teamflow.integrations.github.domain.commits

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class CommitFile(
    sha: Option[String],
    filename: String,
    status: String,
    additions: Int,
    deletions: Int,
    changes: Int,
    blobUrl: Option[String],
    rawUrl: Option[String],
    contentsUrl: Option[String],
    patch: Option[String],
    previousFilename: Option[String]
)

object CommitFile {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}