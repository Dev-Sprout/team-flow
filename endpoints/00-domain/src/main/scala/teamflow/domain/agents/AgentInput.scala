package teamflow.domain.agents

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.syntax.circe._

@JsonCodec
case class AgentInput(
    name: NonEmptyString,
    prompt: NonEmptyString,
    description: Option[NonEmptyString],
  )
