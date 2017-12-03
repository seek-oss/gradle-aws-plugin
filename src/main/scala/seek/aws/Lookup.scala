package seek.aws

import cats.effect.IO
import org.gradle.api._
import seek.aws.cloudformation.CloudFormationStackOutputLookup

trait Lookup {
  def run(p: Project): IO[String]
}

object Lookup {
  def lookup(key: String): Lookup =
    new ProjectLookup(key)

  def stackOutput(stackName: String, key: String): Lookup =
    new CloudFormationStackOutputLookup(stackName, key)
}
