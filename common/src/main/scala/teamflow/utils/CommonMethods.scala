package teamflow.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CommonMethods {
  def parseLdt(dateStr: String): LocalDateTime =
    LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"))
}
