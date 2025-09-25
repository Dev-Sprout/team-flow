package teamflow.repositories

import cats.effect.Resource
import skunk._
import teamflow.Phone
import teamflow.Username
import teamflow.domain.auth.AccessCredentials
import teamflow.domain.auth.AuthedUser.User
import teamflow.repositories.sql.UsersSql
import teamflow.support.skunk.syntax.all._

trait UsersRepository[F[_]] {
  def find(username: Username): F[Option[AccessCredentials[User]]]
  def create(userAndHash: AccessCredentials[User]): F[Unit]
}

object UsersRepository {
  def make[F[_]: fs2.Compiler.Target](
      implicit
      session: Resource[F, Session[F]]
    ): UsersRepository[F] = new UsersRepository[F] {
    override def find(username: Username): F[Option[AccessCredentials[User]]] =
      UsersSql.findByLogin.queryOption(username)

    override def create(userAndHash: AccessCredentials[User]): F[Unit] =
      UsersSql.insert.execute(userAndHash)
  }
}
