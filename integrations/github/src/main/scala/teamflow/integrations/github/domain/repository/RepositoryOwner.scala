package teamflow.integrations.github.domain.repository

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class RepositoryOwner(
    login: String,
    id: Long,
    nodeId: String,
    avatarUrl: String,
    gravatarId: String,
    url: String,
    htmlUrl: String,
    followersUrl: String,
    followingUrl: String,
    gistsUrl: String,
    starredUrl: String,
    subscriptionsUrl: String,
    organizationsUrl: String,
    reposUrl: String,
    eventsUrl: String,
    receivedEventsUrl: String,
    `type`: String,
    siteAdmin: Boolean
)

object RepositoryOwner {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames.withDefaults
}