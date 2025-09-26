package teamflow.repositories.sql

import shapeless.HNil
import skunk._
import skunk.codec.all.bool
import skunk.implicits._
import teamflow.Username
import teamflow.domain.UserId
import teamflow.domain.auth.AccessCredentials
import teamflow.domain.auth.AuthedUser.User
import teamflow.domain.users.UserFilter
import teamflow.support.skunk.Sql
import teamflow.support.skunk.codecs.email
import teamflow.support.skunk.codecs.nes
import teamflow.support.skunk.codecs.username
import teamflow.support.skunk.codecs.zonedDateTime
import teamflow.support.skunk.syntax.all.skunkSyntaxFragmentOps

private[repositories] object UsersSql extends Sql[UserId] {
  private[repositories] val codec =
    (id *: zonedDateTime *: nes *: nes *: email *: username *: bool *: role *: position.opt)
      .to[User]

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
        id, created_at, first_name, last_name, email, username, is_github_member, role, position, password
      FROM users
      WHERE
        username = $username
        AND deleted_at IS NULL
    """.query(personDecoder)

  val insert: Command[AccessCredentials[User]] =
    sql"""
      INSERT INTO users (id, created_at, first_name, last_name, email, username, is_github_member, role, position, password)
      VALUES (
        $id,
        $zonedDateTime,
        $nes,
        $nes,
        $email,
        $username,
        $bool,
        $role,
        ${position.opt},
        $passwordHash
      )
    """
      .command
      .contramap { (u: AccessCredentials[User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstName *: u.data.lastName *: u.data.email *: u.data.username *:
          u.data.isGithubMember *: u.data.role *: u.data.position *: u.password *: EmptyTuple
      }

  def getByFilter(filter: UserFilter): AppliedFragment = {
    val searchFilter: List[Option[AppliedFragment]] = List(
      filter.fullName.map(sql"first_name ILIKE '%' || $nes || '%'"),
      filter.role.map(sql"role = $role"),
      filter.isGithubMember.map(sql"is_github_member = $bool"),
      filter.position.map(sql"position = $position"),
    )

    val query: AppliedFragment =
      void"""
        SELECT
          id, created_at, first_name, last_name, email, username, is_github_member, role, position, COUNT(*) OVER()
        FROM users
      """

    query.whereAndOpt(searchFilter) |+| void""" ORDER BY created_at DESC"""
  }

  val findById: Query[UserId, User] =
    sql"""
      SELECT
        id, created_at, first_name, last_name, email, username, is_github_member, role, position
      FROM users
      WHERE id = $id AND deleted_at IS NULL
      LIMIT 1
    """.query(codec)

  val update: Command[User] =
    sql"""
      UPDATE users SET
        first_name = $nes,
        last_name = $nes,
        email = $email,
        username = $username,
        is_github_member = $bool,
        role = $role,
        position = ${position.opt}
      WHERE id = $id
    """
      .command
      .contramap { (u: User) =>
        u.firstName *: u.lastName *: u.email *: u.username *: u.isGithubMember *: u.role *: u.position *: u.id *: EmptyTuple
      }

  val delete: Command[UserId] =
    sql"""UPDATE users SET deleted_at = NOW() WHERE id = $id""".command
}
