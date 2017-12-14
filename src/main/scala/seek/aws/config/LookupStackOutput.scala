package seek.aws.config

import cats.effect.IO
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import org.gradle.api.Project
import seek.aws.instances._
import seek.aws.syntax._

class LookupStackOutput(stackName: String, outputKey: String) extends Lookup {
  import seek.aws.cloudformation.CloudFormation.stackOutput

  def run(p: Project): IO[String] =
    for {
      r <- p.awsExt.region.run
      c <- IO.pure(AmazonCloudFormationClientBuilder.standard().withRegion(r).build())
      o <- stackOutput(stackName, outputKey).run(c)
    } yield o
}
