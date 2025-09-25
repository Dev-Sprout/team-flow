package teamflow

case class JobsEnvironment[F[_]](
    repos: JobsEnvironment.Repositories[F]
  )

object JobsEnvironment {
  case class Repositories[F[_]](
    )
}
