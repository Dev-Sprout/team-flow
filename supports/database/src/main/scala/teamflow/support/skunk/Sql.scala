package teamflow.support.skunk

import skunk.Codec
import teamflow.effects.IsUUID
import teamflow.support.skunk.codecs.identification

abstract class Sql[T: IsUUID] {
  val id: Codec[T] = identification[T]
}
