package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.commits.Response
import teamflow.support.sttp.SttpRequest

case class GetCommits(
    repo: String,
    owner: String,
    token: String
  )

object GetCommits {
  implicit val sttpRequest: SttpRequest[GetCommits, List[Response]] =
    new SttpRequest[GetCommits, List[Response]] {
      val method: Method = Method.GET
      override def path: Path = r => "repos/" + r.owner + "/" + r.repo + "/commits"
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}
