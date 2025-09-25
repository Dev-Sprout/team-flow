package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.Response
import teamflow.support.sttp.SttpRequest

case class GetCommits(
    repo: String,
    author: String,
    token: String
  )

object GetCommits {
  implicit val sttpRequest: SttpRequest[GetCommits, List[Response]] =
    new SttpRequest[GetCommits, List[Response]] {
      val method: Method = Method.GET
      override def path: Path = r => r.author + "/" + r.repo + "/commits"
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}
