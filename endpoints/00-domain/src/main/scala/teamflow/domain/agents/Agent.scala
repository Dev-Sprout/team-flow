package teamflow.domain.agents

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import teamflow.domain.AgentId
import teamflow.syntax.circe._

@JsonCodec
case class Agent(
    id: AgentId,
    createdAt: ZonedDateTime,
    name: NonEmptyString,
    prompt: NonEmptyString,
    description: Option[NonEmptyString],
  )
