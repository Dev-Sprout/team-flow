package teamflow.integrations.github.domain.users

import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class GitHubUser(
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
    name: Option[String],
    company: Option[String],
    blog: String,
    location: Option[String],
    email: Option[String],
    hireable: Option[Boolean],
    bio: Option[String],
    twitterUsername: Option[String],
    publicRepos: Int,
    publicGists: Int,
    followers: Int,
    following: Int,
    createdAt: String,
    updatedAt: String,
  )

object GitHubUser {
  implicit val configuration: io.circe.generic.extras.Configuration =
    io.circe.generic.extras.Configuration.default.withSnakeCaseMemberNames.withDefaults
}
