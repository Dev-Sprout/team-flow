package teamflow.domain.agents

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class AgentFilter(
    name: Option[NonEmptyString],
    limit: Option[PosInt],
    page: Option[PosInt],
  )
