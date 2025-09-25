package teamflow.utils

import derevo.{Derivation, NewTypeDerivation}
import teamflow.effects.IsUUID

import scala.annotation.implicitNotFound

object uuid extends Derivation[IsUUID] with NewTypeDerivation[IsUUID] {
  def instance(implicit ev: OnlyNewtypes): Nothing = ev.absurd

  @implicitNotFound("Only newtypes instances can be derived")
  final abstract class OnlyNewtypes {
    def absurd: Nothing = ???
  }
}
