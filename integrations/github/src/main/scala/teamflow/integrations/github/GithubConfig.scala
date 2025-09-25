package teamflow.integrations.github

import java.net.URI

import eu.timepit.refined.types.string.NonEmptyString

case class GithubConfig(
    enabled: Boolean,
    apiUrl: URI,
    token: NonEmptyString,
    owner: NonEmptyString
  )
