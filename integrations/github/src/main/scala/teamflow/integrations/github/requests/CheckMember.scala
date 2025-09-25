package teamflow.integrations.github.requests

import sttp.model.{Method, StatusCode}
import teamflow.support.sttp.SttpRequest

case class CheckMember(
    username: String,
    owner: String,
    token: String
  )

object CheckMember {
  implicit val sttpRequest: SttpRequest[CheckMember, StatusCode] =
    new SttpRequest[CheckMember, StatusCode] {
      val method: Method = Method.GET
      override def path: Path = r => "orgs/" + r.owner + "/members/" + r.username
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}
