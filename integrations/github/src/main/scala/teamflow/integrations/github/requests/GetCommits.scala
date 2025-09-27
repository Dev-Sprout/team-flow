package teamflow.integrations.github.requests

import sttp.model.Method
import teamflow.integrations.github.domain.commits.Response
import teamflow.support.sttp.SttpRequest

import java.time.LocalDate

case class GetCommits(
    repo: String,
    owner: String,
    token: String,
    from: LocalDate,
    to: LocalDate
  )

object GetCommits {
  implicit val sttpRequest: SttpRequest[GetCommits, List[Response]] =
    new SttpRequest[GetCommits, List[Response]] {
      val method: Method = Method.GET
      override def path: Path = r => "repos/" + r.owner + "/" + r.repo + "/commits"
      override def params: Params = r => {
        Map(
          "Authorization" -> r.token,
          "since" -> r.from.atStartOfDay().toString,
          "until" -> r.to.atStartOfDay().toString
        )
      }

      def body: Body = noBody
    }
}
