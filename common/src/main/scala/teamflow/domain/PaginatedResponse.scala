package teamflow.domain

import io.circe.generic.JsonCodec

import teamflow.syntax.circe._

@JsonCodec
case class PaginatedResponse[A](
    data: List[A],
    total: Long,
  )
