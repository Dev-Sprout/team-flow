package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.members.Member
import teamflow.support.sttp.SttpRequest

case class GetMembers(
    owner: String,
    token: String
  )

object GetMembers {
  implicit val sttpRequest: SttpRequest[GetMembers, List[Member]] =
    new SttpRequest[GetMembers, List[Member]] {
      val method: Method = Method.GET
      override def path: Path = r => "orgs/" + r.owner + "/members"
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}
