package teamflow.integrations.github.domain

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class User(
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
    siteAdmin: Boolean,
  )

object User {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames
}
