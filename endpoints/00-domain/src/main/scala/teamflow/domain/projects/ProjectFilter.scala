package teamflow.domain.projects

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.syntax.circe._

@JsonCodec
case class ProjectFilter(
    name: Option[NonEmptyString],
    limit: Option[PosInt],
    page: Option[PosInt],
  )
