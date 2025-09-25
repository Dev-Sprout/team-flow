package teamflow.integrations.github.domain.commits

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class CommitDetails(
    sha: String,
    nodeId: String,
    commit: Commit,
    url: String,
    htmlUrl: String,
    commentsUrl: String,
    author: User,
    committer: User,
    parents: List[Parent],
    stats: CommitStats,
    files: List[CommitFile]
)

object CommitDetails {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}