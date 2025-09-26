package teamflow.domain.projects

import java.net.URI

import io.circe.generic.JsonCodec
import teamflow.syntax.circe._

@JsonCodec
case class ProjectInput(
    url: URI
  )
