package seek.aws

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers, Suite}

trait SeekSpecLike extends Suite
  with GeneratorDrivenPropertyChecks
  with Matchers

abstract class SeekSpec extends FreeSpec with SeekSpecLike
