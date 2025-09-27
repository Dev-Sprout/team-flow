package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.users.GitHubUser
import teamflow.support.sttp.SttpRequest

case class GetUser(
    username: String,
    token: String,
  )

object GetUser {
  implicit val sttpRequest: SttpRequest[GetUser, GitHubUser] =
    new SttpRequest[GetUser, GitHubUser] {
      val method: Method = Method.GET
      override def path: Path = r => "users/" + r.username
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}
