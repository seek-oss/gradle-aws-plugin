package seek.aws.config

import cats.effect.IO
import org.gradle.api._

trait Lookup {
  def run(p: Project): IO[String]
}

object Lookup {
  def lookup(key: String): Lookup =
    new LookupProject(key)

  def stackOutput(stackName: String, key: String): Lookup =
    new LookupStackOutput(stackName, key)

  def parameterStore(key: String): Lookup =
    new LookupParameterStore(key)
}
