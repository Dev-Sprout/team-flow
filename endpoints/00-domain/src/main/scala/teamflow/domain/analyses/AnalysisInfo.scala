package teamflow.domain.analyses

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.domain.AnalysisId
import teamflow.domain.agents.Agent
import teamflow.domain.auth.AuthedUser.User
import teamflow.domain.projects.Project
import teamflow.syntax.circe._

@JsonCodec
case class AnalysisInfo(
    id: AnalysisId,
    createdAt: ZonedDateTime,
    project: Project,
    agent: Agent,
    users: List[User],
    response: NonEmptyString,
  )
