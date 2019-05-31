package seek.aws.config

import cats.data.OptionT._
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import com.amazonaws.services.cloudformation.{AmazonCloudFormation, AmazonCloudFormationClientBuilder}
import org.gradle.api.{GradleException, Project}
import seek.aws
import seek.aws.instances._

import scala.collection.JavaConverters._

case class LookupStackOutput(stackName: String, key: String) extends Lookup {

  def runOptional(p: Project): OptionT[IO, String] =
    for {
      c <- liftF(client(p))
      o <- getStackOutput(stackName, key).run(c)
    } yield o

  private def getStackOutput(stackName: String, key: String) =
    Kleisli[OptionT[IO, ?], AmazonCloudFormation, String] { c =>
      OptionT(IO(c.describeStacks(new DescribeStacksRequest().withStackName(stackName))).map { r =>
        r.getStacks.asScala.headOption match {
          case None    => throw new GradleException(s"Stack ${stackName} does not exist")
          case Some(h) => h.getOutputs.asScala.find(_.getOutputKey == key) match {
            case None    => None
            case Some(o) => Some(o.getOutputValue)
          }
        }
      })
    }

  private def client(p: Project): IO[AmazonCloudFormation] =
    aws.client.build(AmazonCloudFormationClientBuilder.standard(), p.awsExt.region, p.awsExt.roleArn)
}
