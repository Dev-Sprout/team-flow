package teamflow

import cats.effect.Async
import cats.effect.Resource
import skunk.Session
import teamflow.repositories._

case class Repositories[F[_]](
    users: UsersRepository[F],
    assetsRepository: AssetsRepository[F],
  )

object Repositories {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): Repositories[F] =
    Repositories(
      users = UsersRepository.make[F],
      assetsRepository = AssetsRepository.make[F],
    )
}
