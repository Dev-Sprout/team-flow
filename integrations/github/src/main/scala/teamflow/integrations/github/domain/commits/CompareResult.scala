package teamflow.integrations.github.domain.commits

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class CompareResult(
    url: String,
    htmlUrl: String,
    permalinkUrl: String,
    diffUrl: String,
    patchUrl: String,
    baseCommit: CompareCommit,
    mergeBaseCommit: CompareCommit,
    status: String,
    aheadBy: Int,
    behindBy: Int,
    totalCommits: Int,
    commits: List[Response],
    files: List[CommitFile]
)

object CompareResult {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}