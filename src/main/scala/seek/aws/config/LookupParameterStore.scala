package seek.aws.config

import cats.effect.IO
import org.gradle.api.Project

class LookupParameterStore(key: String) extends Lookup {

  def run(p: Project): IO[String] = ???
}
