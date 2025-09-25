package teamflow.integrations.github.domain

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class Response(
    sha: String,
    nodeId: String,
    commit: Commit,
    url: String,
    htmlUrl: String,
    commentsUrl: String,
    author: User,
    committer: User,
    parents: List[Parent],
  )

object Response {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}
