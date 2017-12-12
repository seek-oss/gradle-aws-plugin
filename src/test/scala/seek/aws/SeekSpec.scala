package seek.aws

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, Suite}

trait SeekSpec extends Suite
  with GeneratorDrivenPropertyChecks
  with Matchers
