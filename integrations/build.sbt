name := "integrations"

lazy val integration_telegram = project.in(file("telegram"))
lazy val integration_anthropic = project.in(file("anthropic"))
lazy val integration_github = project.in(file("github"))
lazy val integration_aws = project.in(file("aws"))

aggregateProjects(
  integration_telegram,
  integration_anthropic,
  integration_github,
  integration_aws,
)
