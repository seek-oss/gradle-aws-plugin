package seek.aws.config

import cats.effect.IO
import org.gradle.api._

trait Lookup {
  def run(p: Project): IO[String]
}

object Lookup {
  def lookup(key: String): Lookup =
    new ProjectLookup(key)

  def stackOutput(stackName: String, key: String): Lookup =
    new CloudFormationStackOutputLookup(stackName, key)
}
