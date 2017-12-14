package seek.aws.config

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.cloudformation.{AmazonCloudFormation, AmazonCloudFormationClientBuilder}
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import org.gradle.api.{GradleException, Project}
import seek.aws.instances._
import seek.aws.syntax._
import scala.collection.JavaConverters._

case class LookupStackOutput(stackName: String, key: String) extends Lookup {

  def run(p: Project): IO[String] =
    for {
      r <- p.awsExt.region.run
      c <- IO.pure(AmazonCloudFormationClientBuilder.standard().withRegion(r).build())
      o <- getStackOutput(stackName, key).run(c)
    } yield o

  private def getStackOutput(stackName: String, key: String): Kleisli[IO, AmazonCloudFormation, String] =
    Kleisli { c =>
      IO(c.describeStacks(new DescribeStacksRequest().withStackName(stackName))).map { r =>
        r.getStacks.asScala.headOption match {
          case None    => throw new GradleException(s"Stack ${stackName} does not exist")
          case Some(h) => h.getOutputs.asScala.find(_.getOutputKey == key) match {
            case None    => throw new LookupKeyNotFound(key)
            case Some(o) => o.getOutputValue
          }
        }
      }
    }

}
