package teamflow.domain.analyses

import java.time.LocalDate
import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.domain.AgentId
import teamflow.domain.AnalysisId
import teamflow.domain.ProjectId
import teamflow.syntax.circe._

@JsonCodec
case class Analysis(
    id: AnalysisId,
    createdAt: ZonedDateTime,
    projectId: ProjectId,
    agentId: AgentId,
    response: NonEmptyString,
    dateFrom: LocalDate,
    dateTo: LocalDate,
    durationSeconds: Option[Long],
  )
