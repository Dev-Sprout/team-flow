package teamflow.integrations.github.domain

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class Commit(
    author: Author,
    committer: Committer,
    message: String,
    tree: Tree,
    url: String,
    commentCount: Int,
    verification: Verification,
  )

object Commit {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}
