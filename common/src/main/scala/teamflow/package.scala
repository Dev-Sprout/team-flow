import cats.Monad
import cats.effect.std.Random
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps}
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

package object teamflow {
  type Username = String Refined MatchesRegex[W.`"""^[A-Za-z\\d-]{1,39}$"""`.T]
  type Phone = String Refined MatchesRegex[W.`"""[+][\\d]{12}+"""`.T]
  type Email = String Refined MatchesRegex[W.`"""^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"""`.T]
  type Digits = String Refined MatchesRegex[W.`"""[\\d]+"""`.T]

  def randomStr[F[_]: Random: Monad](n: Int, cond: Char => Boolean = _ => true): F[String] = {
    def makeString(size: Int, string: String): F[String] =
      if (size == 0) string.pure[F]
      else {
        val charF = Random[F].nextAlphaNumeric
        Monad[F]
          .ifM(charF.map(cond))(
            charF.flatMap(char => makeString(size - 1, string + char)),
            makeString(size, string),
          )
      }
    makeString(n, "")
  }
}
