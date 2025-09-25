package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.commits.CompareResult
import teamflow.support.sttp.SttpRequest

case class GetCompare(
    owner: String,
    repo: String,
    base: String,
    head: String,
    token: String
)

object GetCompare {
  implicit val sttpRequest: SttpRequest[GetCompare, CompareResult] =
    new SttpRequest[GetCompare, CompareResult] {
      val method: Method = Method.GET
      override def path: Path = r => "repos/" + r.owner + "/" + r.repo + "/compare/" + r.base + "..." + r.head
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}