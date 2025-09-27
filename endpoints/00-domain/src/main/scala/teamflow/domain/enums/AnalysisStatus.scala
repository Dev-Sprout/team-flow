package teamflow.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait AnalysisStatus extends Snakecase
object AnalysisStatus extends Enum[AnalysisStatus] with CirceEnum[AnalysisStatus] {
  case object Started extends AnalysisStatus
  case object GetCommits extends AnalysisStatus
  case object GetCommitDetails extends AnalysisStatus
  case object Analyzing extends AnalysisStatus
  case object Success extends AnalysisStatus
  case object Failed extends AnalysisStatus
  override def values: IndexedSeq[AnalysisStatus] = findValues
}
