package teamflow.generators

import org.scalacheck.Gen
import teamflow.Language
import teamflow.domain.enums.Role
import teamflow.test.generators.Generators

trait TypeGen { this: Generators =>
  val roleGen: Gen[Role] = Gen.oneOf(Role.values)
  val languageGen: Gen[Language] = Gen.oneOf(Language.values)
}
