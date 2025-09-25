package teamflow.domain.projects

import java.net.URI

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.domain.ProjectId
import teamflow.syntax.circe._

@JsonCodec
case class Project(
    id: ProjectId,
    name: Option[NonEmptyString],
    url: URI,
  )
