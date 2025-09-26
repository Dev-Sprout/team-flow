package teamflow.domain.projects

import cats.data.NonEmptyList
import io.circe.generic.JsonCodec
import teamflow.domain.AgentId
import teamflow.domain.ProjectId
import teamflow.domain.UserId
import teamflow.syntax.circe._

@JsonCodec
case class ProjectAnalyze(
    projectId: ProjectId,
    agentId: AgentId,
    userIds: NonEmptyList[UserId],
  )
