package teamflow.integrations.github

import cats.Applicative
import cats.effect.Sync
import cats.implicits.toFunctorOps
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger
import teamflow.integrations.github.domain.Response
import teamflow.integrations.github.requests.GetCommits
import teamflow.support.sttp.SttpBackends
import teamflow.support.sttp.SttpClient
import teamflow.support.sttp.SttpClientAuth
import java.net.URI

trait GithubClient[F[_]] {
  def getCommits(repo: NonEmptyString, author: NonEmptyString): F[List[Response]]
}

object GithubClient {
  def make[F[_]: Sync: Logger: SttpBackends.Simple](token: Option[NonEmptyString]): GithubClient[F] =
    if (token.nonEmpty)
      new GithubClientImpl[F](token.get.value)
    else
      new NoOpGithubClientImpl[F]

  private class GithubClientImpl[F[_]: Sync: SttpBackends.Simple](token: String)
      extends GithubClient[F] {
    private lazy val client: SttpClient.CirceJson[F] =
      SttpClient.circeJson(
        URI.create("https://api.github.com/repos/"),
        SttpClientAuth.noAuth,
      )
    override def getCommits(repo: NonEmptyString, author: NonEmptyString): F[List[Response]] =
      client.request(GetCommits(repo.value, author.value, token))
  }

  private class NoOpGithubClientImpl[F[_]: Applicative](implicit logger: Logger[F])
      extends GithubClient[F] {
    override def getCommits(repo: NonEmptyString, author: NonEmptyString): F[List[Response]] =
      logger.info(s"Getting commits [$repo]").map(_ => List.empty)
  }
}
