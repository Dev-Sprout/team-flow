package teamflow.integrations.github.domain.members

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class Member(
    login: String,
    id: Long,
    nodeId: String,
    avatarUrl: String,
    gravatarId: Option[String],
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
    memberType: String,
    siteAdmin: Boolean
)

object Member {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames.withDefaults
}