package teamflow.domain.analyses

import io.circe.generic.JsonCodec
import teamflow.domain.AnalysisId
import teamflow.domain.UserId
import teamflow.syntax.circe._

@JsonCodec
case class UserAnalysis(
    userId: UserId,
    analysisId: AnalysisId,
  )
