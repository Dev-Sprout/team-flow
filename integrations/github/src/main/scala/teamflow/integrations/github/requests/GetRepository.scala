package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.repository.Repository
import teamflow.support.sttp.SttpRequest

case class GetRepository(
    owner: String,
    repo: String,
    token: String
)

object GetRepository {
  implicit val sttpRequest: SttpRequest[GetRepository, Repository] =
    new SttpRequest[GetRepository, Repository] {
      val method: Method = Method.GET
      override def path: Path = r => "repos/" + r.owner + "/" + r.repo
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}