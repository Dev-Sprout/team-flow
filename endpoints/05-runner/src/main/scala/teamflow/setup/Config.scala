package teamflow.setup

import teamflow.auth.AuthConfig
import teamflow.integration.aws.s3.AWSConfig
import teamflow.integrations.anthropic.AnthropicConfig
import teamflow.integrations.github.GithubConfig
import teamflow.support.database.MigrationsConfig
import teamflow.support.http4s.HttpServerConfig
import teamflow.support.jobs.JobsRunnerConfig
import teamflow.support.redis.RedisConfig
import teamflow.support.skunk.DataBaseConfig

case class Config(
    httpServer: HttpServerConfig,
    database: DataBaseConfig,
    auth: AuthConfig,
    redis: RedisConfig,
    s3: AWSConfig,
    jobs: JobsRunnerConfig,
    github: GithubConfig,
    anthropic: AnthropicConfig,
  ) {
  lazy val migrations: MigrationsConfig = MigrationsConfig(
    hostname = database.host.value,
    port = database.port.value,
    database = database.database.value,
    username = database.user.value,
    password = database.password.value,
    schema = "public",
    location = "db/migration",
  )
}
