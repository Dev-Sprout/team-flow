package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.commits.CommitDetails
import teamflow.support.sttp.SttpRequest

case class GetCommitDetails(
    owner: String,
    repo: String,
    sha: String,
    token: String
)

object GetCommitDetails {
  implicit val sttpRequest: SttpRequest[GetCommitDetails, CommitDetails] =
    new SttpRequest[GetCommitDetails, CommitDetails] {
      val method: Method = Method.GET
      override def path: Path = r => "repos/" + r.owner + "/" + r.repo + "/commits/" + r.sha
      override def params: Params = r => Map("Authorization" -> r.token)

      def body: Body = noBody
    }
}