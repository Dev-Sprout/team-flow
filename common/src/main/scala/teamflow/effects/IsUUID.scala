package teamflow.effects

import monocle.Iso

import java.util.UUID

trait IsUUID[A] {
  def uuid: Iso[UUID, A]
}

object IsUUID {
  def apply[A: IsUUID]: IsUUID[A] = implicitly

  implicit val identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    val uuid: Iso[UUID, UUID] = Iso[UUID, UUID](identity)(identity)
  }
}
