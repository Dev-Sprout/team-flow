package teamflow.domain.analyses

import java.time.LocalDate

import cats.data.NonEmptyList
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.domain.AgentId
import teamflow.domain.ProjectId
import teamflow.domain.UserId
import teamflow.syntax.circe._

@JsonCodec
case class AnalysisInput(
    projectId: ProjectId,
    agentId: AgentId,
    userIds: NonEmptyList[UserId],
    from: LocalDate,
    to: LocalDate,
  )
