package teamflow.repositories

import skunk._
import skunk.codec.all._
import skunk.data.Type
import teamflow.domain.enums.Position
import teamflow.domain.enums.Role
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

package object sql {
  val role: Codec[Role] = `enum`[Role](Role, Type("role"))
  val position: Codec[Position] = `enum`[Position](Position, Type("job_position"))

  val passwordHash: Codec[PasswordHash[SCrypt]] =
    varchar.imap[PasswordHash[SCrypt]](PasswordHash[SCrypt])(identity)
}
