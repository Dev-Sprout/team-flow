package teamflow.domain

import java.util.UUID

import io.circe.generic.JsonCodec

@JsonCodec
case class Response(
    id: UUID,
    message: String,
  )
