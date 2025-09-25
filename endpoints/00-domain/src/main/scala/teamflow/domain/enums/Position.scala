package teamflow.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait Position extends Snakecase
object Position extends Enum[Position] with CirceEnum[Position] {
  case object ProjectManager extends Position
  case object FullStackDeveloper extends Position
  case object FrontEndDeveloper extends Position
  case object BackendDeveloper extends Position
  case object AIEngineer extends Position
  case object DataAnalyst extends Position
  case object DataEngineer extends Position
  case object QA extends Position
  case object Designer extends Position
  case object HR extends Position
  case object Accountant extends Position
  case object Other extends Position
  override def values: IndexedSeq[Position] = findValues
}
