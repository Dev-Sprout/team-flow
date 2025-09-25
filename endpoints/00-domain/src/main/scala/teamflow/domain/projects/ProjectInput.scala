package teamflow.domain.projects

import java.net.URI

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.syntax.circe._

@JsonCodec
case class ProjectInput(
    name: Option[NonEmptyString],
    url: URI,
  )
