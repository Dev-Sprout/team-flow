package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.support.sttp.SttpRequest

case class GetRawContent(
    owner: String,
    repo: String,
    sha: String,
    path: String,
    token: String
)

object GetRawContent {
  implicit val sttpRequest: SttpRequest[GetRawContent, String] =
    new SttpRequest[GetRawContent, String] {
      val method: Method = Method.GET
      override def path: Path = r => "repos/" + r.owner + "/" + r.repo + "/" + r.sha + "/" + r.path
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}