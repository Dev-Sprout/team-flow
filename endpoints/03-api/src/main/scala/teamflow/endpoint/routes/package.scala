package teamflow.endpoint

import org.http4s.AuthedRequest
import org.http4s.Request
import teamflow.domain.auth.AuthedUser
import teamflow.domain.enums.Role

package object routes {
  object asAdmin {
    def unapply[F[_]](ar: AuthedRequest[F, AuthedUser]): Option[(Request[F], AuthedUser)] =
      Option.when(ar.context.role == Role.Admin)(
        ar.req -> ar.context
      )
  }
}
