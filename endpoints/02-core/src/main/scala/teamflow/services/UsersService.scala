package teamflow.services

import cats.data.OptionT
import cats.effect.Sync
import cats.effect.std.Random
import cats.implicits._
import teamflow.Username
import teamflow.domain.PaginatedResponse
import teamflow.domain.Response
import teamflow.domain.UserId
import teamflow.domain.auth.AccessCredentials
import teamflow.domain.auth.AuthedUser.User
import teamflow.domain.users.UserFilter
import teamflow.domain.users.UserInput
import teamflow.effects.Calendar
import teamflow.effects.FileLoader
import teamflow.effects.GenUUID
import teamflow.integrations.github.GithubClient
import teamflow.repositories.UsersRepository
import teamflow.utils.ID
import tsec.passwordhashers.jca.SCrypt

trait UsersService[F[_]] {
  def create(input: UserInput): F[Response]
  def check(username: Username): F[Boolean]
  def get(filters: UserFilter): F[PaginatedResponse[User]]
  def find(id: UserId): F[Option[User]]
  def update(id: UserId, input: UserInput): F[Response]
  def delete(id: UserId): F[Response]
}

object UsersService {
  def make[F[_]: Sync: FileLoader: GenUUID: Calendar: Random](
      usersRepo: UsersRepository[F],
      githubClient: GithubClient[F],
    ): UsersService[F] =
    new UsersService[F] {
      override def create(input: UserInput): F[Response] =
        for {
          id <- ID.make[F, UserId]
          now <- Calendar[F].currentZonedDateTime
          rawPass <- Random[F].betweenInt(1000, 9999).map(_.toString)
          hash <- SCrypt.hashpw[F](rawPass)
          isGithubMember <- check(input.username)
          avatarUrl <- if (isGithubMember) getGithubAvatar(input.username.value) else None.pure[F]
          user = User(
            id = id,
            createdAt = now,
            firstName = input.firstName,
            lastName = input.lastName,
            email = input.email,
            username = input.username,
            isGithubMember = isGithubMember,
            role = input.role,
            position = input.position,
            avatarUrl = avatarUrl,
          )
          userAndHash = AccessCredentials(user, hash)
          _ <- usersRepo.create(userAndHash)
        } yield Response(id.value, s"${user.username} created with password $rawPass")

      override def check(username: Username): F[Boolean] =
        githubClient.checkMember(username.value).map(_.isSuccess)

      private def getGithubAvatar(username: String): F[Option[String]] =
        githubClient.getUser(username).map(user => Some(user.avatarUrl))

      override def get(filters: UserFilter): F[PaginatedResponse[User]] =
        usersRepo.get(filters)

      override def find(id: UserId): F[Option[User]] =
        usersRepo.findById(id)

      override def update(id: UserId, input: UserInput): F[Response] =
        OptionT(find(id))
          .semiflatMap { existing =>
            val updated = existing.copy(
              firstName = input.firstName,
              lastName = input.lastName,
              email = input.email,
              username = input.username,
              role = input.role,
              position = input.position,
            )
            usersRepo.update(updated)
            Response(id.value, s"${updated.username} updated").pure[F]
          }
          .getOrElse(Response(id.value, s"User with id $id not found"))

      override def delete(id: UserId): F[Response] =
        OptionT(find(id))
          .semiflatMap { existing =>
            usersRepo.delete(id)
            Response(id.value, s"${existing.username} deleted").pure[F]
          }
          .getOrElse(Response(id.value, s"User with id $id not found"))
    }
}
