package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.contents.Content
import teamflow.support.sttp.SttpRequest

case class GetContents(
    owner: String,
    repo: String,
    path: String,
    ref: Option[String],
    token: String
  )

object GetContents {
  implicit val sttpRequest: SttpRequest[GetContents, Content] =
    new SttpRequest[GetContents, Content] {
      val method: Method = Method.GET
      override def path: Path = r => "repos/" + r.owner + "/" + r.repo + "/contents/" + r.path
      override def params: Params = r => {
        val baseParams = Map("Authorization" -> r.token)
        r.ref.fold(baseParams)(ref => baseParams + ("ref" -> ref))
      }

      def body: Body = noBody
    }
}