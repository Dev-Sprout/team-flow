package teamflow.repositories.sql

import shapeless.HNil
import skunk._
import skunk.implicits._
import teamflow.Phone
import teamflow.Username
import teamflow.domain.UserId
import teamflow.domain.auth.AccessCredentials
import teamflow.domain.auth.AuthedUser.User
import teamflow.support.skunk.Sql
import teamflow.support.skunk.codecs.email
import teamflow.support.skunk.codecs.nes
import teamflow.support.skunk.codecs.phone
import teamflow.support.skunk.codecs.username
import teamflow.support.skunk.codecs.zonedDateTime

private[repositories] object UsersSql extends Sql[UserId] {
  private val codec =
    (id *: zonedDateTime *: nes *: nes *: email *: username *: role *: position.opt).to[User]

  private val personDecoder: Decoder[AccessCredentials[User]] =
    (codec *: passwordHash).map {
      case user *: hash *: HNil =>
        AccessCredentials(
          data = user,
          password = hash,
        )
    }

  val findByLogin: Query[Username, AccessCredentials[User]] =
    sql"""
      SELECT
        id, created_at, first_name, last_name, email, username, role, position, password
      FROM users
      WHERE
        username = $username
        AND deleted_at IS NULL
    """.query(personDecoder)

  val insert: Command[AccessCredentials[User]] =
    sql"""
      INSERT INTO users (id, created_at, first_name, last_name, email, username, role, position, password)
      VALUES (
        $id,
        $zonedDateTime,
        $nes,
        $nes,
        $email,
        $username,
        $role,
        ${position.opt},
        $passwordHash
      )
    """
      .command
      .contramap { (u: AccessCredentials[User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstName *: u.data.lastName *: u.data.email *:
          u.data.username *: u.data.role *: u.data.position *: u.password *: EmptyTuple
      }

  val delete: Command[UserId] =
    sql"""UPDATE users SET deleted_at = NOW() WHERE id = $id""".command
}
