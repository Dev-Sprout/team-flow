package teamflow

import cats.data.OptionT
import cats.effect.Async
import cats.effect.std.Random
import org.typelevel.log4cats.Logger
import teamflow.auth.AuthConfig
import teamflow.auth.impl.Auth
import teamflow.domain.auth.AccessCredentials
import teamflow.domain.auth.AuthedUser
import teamflow.integration.aws.s3.S3Client
import teamflow.integrations.anthropic.AnthropicClient
import teamflow.integrations.github.GithubClient
import teamflow.services._
import teamflow.support.redis.RedisClient

case class Services[F[_]](
    auth: Auth[F, AuthedUser],
    assets: AssetsService[F],
    users: UsersService[F],
    agents: AgentsService[F],
    projects: ProjectsService[F],
  )

object Services {
  def make[F[_]: Async: Logger: Random](
      config: AuthConfig,
      repositories: Repositories[F],
      redis: RedisClient[F],
      s3Client: S3Client[F],
      githubClient: GithubClient[F],
      anthropicClient: AnthropicClient[F],
    ): Services[F] = {
    def findUser: Username => F[Option[AccessCredentials[AuthedUser]]] = username =>
      OptionT(repositories.users.find(username))
        .map(identity[AccessCredentials[AuthedUser]])
        .value

    Services[F](
      auth = Auth.make[F, AuthedUser](config.user, findUser, redis),
      assets = AssetsService.make[F](
        repositories.assetsRepository,
        s3Client,
      ),
      users = UsersService.make[F](repositories.users, githubClient),
      agents = AgentsService.make[F](repositories.agents),
      projects = ProjectsService.make[F](
        repositories.projects,
        repositories.agents,
        repositories.users,
        githubClient,
        anthropicClient,
      ),
    )
  }
}
