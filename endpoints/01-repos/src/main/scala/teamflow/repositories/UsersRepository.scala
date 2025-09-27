package teamflow.repositories

import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import teamflow.Username
import teamflow.domain.PaginatedResponse
import teamflow.domain.UserId
import teamflow.domain.auth.AccessCredentials
import teamflow.domain.auth.AuthedUser.User
import teamflow.domain.users.UserFilter
import teamflow.repositories.sql.UsersSql
import teamflow.support.skunk.syntax.all._

trait UsersRepository[F[_]] {
  def get(filter: UserFilter): F[PaginatedResponse[User]]
  def findById(id: UserId): F[Option[User]]
  def findByIds(ids: List[UserId]): F[Map[UserId, User]]
  def find(username: Username): F[Option[AccessCredentials[User]]]
  def create(userAndHash: AccessCredentials[User]): F[Unit]
  def update(user: User): F[Unit]
  def delete(id: UserId): F[Unit]
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

    override def get(filter: UserFilter): F[PaginatedResponse[User]] = {
      val af = UsersSql.getByFilter(filter).paginateOpt(filter.limit, filter.page)
      for {
        users <- af.fragment.query(UsersSql.codec *: int8).queryList(af.argument)
        list = users.map(_.head)
        count = users.headOption.fold(0L)(_.tail.head)
      } yield PaginatedResponse(list, count)
    }

    override def findById(id: UserId): F[Option[User]] =
      UsersSql.findById.queryOption(id)

    override def findByIds(ids: List[UserId]): F[Map[UserId, User]] =
      if (ids.isEmpty) Map.empty[UserId, User].pure[F]
      else
        UsersSql.findByIds(ids).map(u => u.id -> u).queryList(ids).map(_.toMap)

    override def update(user: User): F[Unit] =
      UsersSql.update.execute(user)

    override def delete(id: UserId): F[Unit] =
      UsersSql.delete.execute(id)
  }
}
