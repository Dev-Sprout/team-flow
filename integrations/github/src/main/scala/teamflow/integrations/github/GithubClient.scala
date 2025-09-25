package teamflow.integrations.github

import cats.Applicative
import cats.effect.Sync
import cats.implicits.toFunctorOps
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger
import teamflow.integrations.github.domain.commits.{Response, CommitDetails, CompareResult}
import teamflow.integrations.github.domain.contents.{Content, ContentLinks}
import teamflow.integrations.github.domain.members.Member
import teamflow.integrations.github.requests.{GetCommits, GetCommitDetails, GetContents, GetMembers, GetRawContent, GetCompare, CheckMember}
import teamflow.support.sttp.SttpBackends
import teamflow.support.sttp.SttpClient
import teamflow.support.sttp.SttpClientAuth
import sttp.model.StatusCode

trait GithubClient[F[_]] {
  def getCommits(repo: NonEmptyString): F[List[Response]]
  def getCommitDetails(repo: NonEmptyString, sha: String): F[CommitDetails]
  def getMembers: F[List[Member]]
  def checkMember(username: String): F[StatusCode]
  def getContents(repo: NonEmptyString, path: String, ref: Option[String] = None): F[Content]
  def getRawContent(repo: NonEmptyString, sha: String, path: String): F[String]
  def getCompare(repo: NonEmptyString, base: String, head: String): F[CompareResult]
}

object GithubClient {
  def make[F[_]: Sync: Logger: SttpBackends.Simple](config: GithubConfig): GithubClient[F] =
    if (config.enabled)
      new GithubClientImpl[F](config)
    else
      new NoOpGithubClientImpl[F]

  private class GithubClientImpl[F[_]: Sync: SttpBackends.Simple](config: GithubConfig)
      extends GithubClient[F] {
    private lazy val client: SttpClient.CirceJson[F] =
      SttpClient.circeJson(
        config.apiUrl,
        SttpClientAuth.noAuth,
      )
    override def getCommits(repo: NonEmptyString): F[List[Response]] =
      client.request(GetCommits(repo.value, config.owner.value, config.token.value))
    
    override def getCommitDetails(repo: NonEmptyString, sha: String): F[CommitDetails] =
      client.request(GetCommitDetails(config.owner.value, repo.value, sha, config.token.value))
    
    override def getMembers: F[List[Member]] =
      client.request(GetMembers(config.owner.value, config.token.value))
    
    override def checkMember(username: String): F[StatusCode] =
      client.request(CheckMember(username, config.owner.value, config.token.value))
    
    override def getContents(repo: NonEmptyString, path: String, ref: Option[String] = None): F[Content] =
      client.request(GetContents(config.owner.value, repo.value, path, ref, config.token.value))
    
    override def getRawContent(repo: NonEmptyString, sha: String, path: String): F[String] =
      client.request(GetRawContent(config.owner.value, repo.value, sha, path, config.token.value))
    
    override def getCompare(repo: NonEmptyString, base: String, head: String): F[CompareResult] =
      client.request(GetCompare(config.owner.value, repo.value, base, head, config.token.value))
  }

  private class NoOpGithubClientImpl[F[_]: Applicative](implicit logger: Logger[F])
      extends GithubClient[F] {
    override def getCommits(repo: NonEmptyString): F[List[Response]] =
      logger.info(s"Getting commits [$repo]").map(_ => List.empty)
    
    override def getMembers: F[List[Member]] =
      logger.info("Getting organization members").map(_ => List.empty)
    
    override def checkMember(username: String): F[StatusCode] =
      logger.info(s"Checking member [$username]").map(_ => StatusCode.Ok)
    
    override def getCommitDetails(repo: NonEmptyString, sha: String): F[CommitDetails] =
      logger.info(s"Getting commit details [$repo] sha [$sha]").map(_ => 
        CommitDetails(
          sha = "",
          nodeId = "",
          commit = teamflow.integrations.github.domain.commits.Commit(
            author = teamflow.integrations.github.domain.commits.Author(
              eu.timepit.refined.refineMV("noop"),
              eu.timepit.refined.refineMV("noop@example.com"),
              java.time.ZonedDateTime.now()
            ),
            committer = teamflow.integrations.github.domain.commits.Committer(
              eu.timepit.refined.refineMV("noop"),
              eu.timepit.refined.refineMV("noop@example.com"),
              java.time.ZonedDateTime.now()
            ),
            message = "",
            tree = teamflow.integrations.github.domain.commits.Tree("", ""),
            url = "",
            commentCount = 0,
            verification = teamflow.integrations.github.domain.commits.Verification(false, "", None, None, None)
          ),
          url = "",
          htmlUrl = "",
          commentsUrl = "",
          author = teamflow.integrations.github.domain.commits.User("", 0, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false),
          committer = teamflow.integrations.github.domain.commits.User("", 0, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false),
          parents = List.empty,
          stats = teamflow.integrations.github.domain.commits.CommitStats(0, 0, 0),
          files = List.empty
        )
      )
    
    override def getContents(repo: NonEmptyString, path: String, ref: Option[String] = None): F[Content] =
      logger.info(s"Getting contents for [$repo] path [$path] ref [${ref.getOrElse("default")}]").map(_ => 
        Content(
          name = "",
          path = "",
          sha = "",
          size = 0L,
          url = "",
          htmlUrl = "",
          gitUrl = "",
          downloadUrl = None,
          contentType = "file",
          content = None,
          encoding = None,
          links = ContentLinks("", "", "")
        )
      )
    
    override def getRawContent(repo: NonEmptyString, sha: String, path: String): F[String] =
      logger.info(s"Getting raw content [$repo] sha [$sha] path [$path]").map(_ => "")
    
    override def getCompare(repo: NonEmptyString, base: String, head: String): F[CompareResult] =
      logger.info(s"Getting compare [$repo] base [$base] head [$head]").map(_ => 
        CompareResult(
          url = "",
          htmlUrl = "",
          permalinkUrl = "",
          diffUrl = "",
          patchUrl = "",
          baseCommit = teamflow.integrations.github.domain.commits.CompareCommit(
            sha = "",
            nodeId = "",
            commit = teamflow.integrations.github.domain.commits.Commit(
              author = teamflow.integrations.github.domain.commits.Author(
                eu.timepit.refined.refineMV("noop"),
                eu.timepit.refined.refineMV("noop@example.com"),
                java.time.ZonedDateTime.now()
              ),
              committer = teamflow.integrations.github.domain.commits.Committer(
                eu.timepit.refined.refineMV("noop"),
                eu.timepit.refined.refineMV("noop@example.com"),
                java.time.ZonedDateTime.now()
              ),
              message = "",
              tree = teamflow.integrations.github.domain.commits.Tree("", ""),
              url = "",
              commentCount = 0,
              verification = teamflow.integrations.github.domain.commits.Verification(false, "", None, None, None)
            ),
            url = "",
            htmlUrl = "",
            commentsUrl = "",
            author = teamflow.integrations.github.domain.commits.User("", 0, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false),
            committer = teamflow.integrations.github.domain.commits.User("", 0, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false),
            parents = List.empty
          ),
          mergeBaseCommit = teamflow.integrations.github.domain.commits.CompareCommit(
            sha = "",
            nodeId = "",
            commit = teamflow.integrations.github.domain.commits.Commit(
              author = teamflow.integrations.github.domain.commits.Author(
                eu.timepit.refined.refineMV("noop"),
                eu.timepit.refined.refineMV("noop@example.com"),
                java.time.ZonedDateTime.now()
              ),
              committer = teamflow.integrations.github.domain.commits.Committer(
                eu.timepit.refined.refineMV("noop"),
                eu.timepit.refined.refineMV("noop@example.com"),
                java.time.ZonedDateTime.now()
              ),
              message = "",
              tree = teamflow.integrations.github.domain.commits.Tree("", ""),
              url = "",
              commentCount = 0,
              verification = teamflow.integrations.github.domain.commits.Verification(false, "", None, None, None)
            ),
            url = "",
            htmlUrl = "",
            commentsUrl = "",
            author = teamflow.integrations.github.domain.commits.User("", 0, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false),
            committer = teamflow.integrations.github.domain.commits.User("", 0, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false),
            parents = List.empty
          ),
          status = "",
          aheadBy = 0,
          behindBy = 0,
          totalCommits = 0,
          commits = List.empty,
          files = List.empty
        )
      )
  }
}
