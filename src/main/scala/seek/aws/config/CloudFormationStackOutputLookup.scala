package seek.aws.config

import cats.effect.IO
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import org.gradle.api.Project
import seek.aws.cloudformation.stackOutput
import seek.aws.instances._
import seek.aws.syntax._

class CloudFormationStackOutputLookup(stackName: String, outputKey: String) extends Lookup {

  def run(p: Project): IO[String] =
    for {
      r <- p.awsExt.region.run
      c <- IO.pure(AmazonCloudFormationClientBuilder.standard().withRegion(r).build())
      o <- stackOutput(stackName, outputKey).run(c)
    } yield o
}
