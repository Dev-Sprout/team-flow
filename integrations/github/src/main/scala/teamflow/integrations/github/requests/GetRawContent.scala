package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.support.sttp.SttpRequest
import teamflow.integrations.github.domain.contents.Content

case class GetRawContent(
    owner: String,
    repo: String,
    sha: String,
    path: String,
    token: String
)

object GetRawContent {
  implicit val sttpRequest: SttpRequest[GetRawContent, Content] =
    new SttpRequest[GetRawContent, Content] {
      val method: Method = Method.GET
      override def path: Path = r => "repos/" + r.owner + "/" + r.repo + "/contents/" + r.path
      override def params: Params = r => Map("Authorization" -> r.token, "ref" -> r.sha)

      def body: Body = noBody
    }
}