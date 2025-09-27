package teamflow.domain.analyses

import eu.timepit.refined.types.numeric.PosInt
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.domain.AgentId
import teamflow.domain.ProjectId
import teamflow.domain.UserId
import teamflow.syntax.circe._

@JsonCodec
case class AnalysisFilter(
    projectId: Option[ProjectId] = None,
    agentId: Option[AgentId] = None,
    userId: Option[UserId] = None,
    limit: Option[PosInt] = None,
    page: Option[PosInt] = None,
  )
