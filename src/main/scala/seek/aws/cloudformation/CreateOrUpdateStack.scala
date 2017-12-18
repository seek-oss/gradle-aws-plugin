package seek.aws
package cloudformation

import cats.data.Kleisli
import cats.data.Kleisli._
import cats.effect.IO
import com.amazonaws.services.cloudformation.model._
import com.amazonaws.services.cloudformation.{AmazonCloudFormation, AmazonCloudFormationClientBuilder}
import seek.aws.cloudformation.instances._
import seek.aws.cloudformation.syntax._

import scala.collection.JavaConverters._

class CreateOrUpdateStack extends AwsTask {
  import CloudFormation._

  setDescription("Creates a CloudFormation stack or updates it if it already exists")

  override def run: IO[Unit] =
    for {
      r  <- region
      to <- project.cfnExt.stackWaitTimeout
      sp <- StackDescription(project)
      c  <- IO.pure(AmazonCloudFormationClientBuilder.standard().withRegion(r).build())
      _  <- createOrUpdate(sp).run(c)
      _  <- waitForStack(sp.name, to).run(c)
    } yield ()

  private def createOrUpdate(sd: StackDescription): Kleisli[IO, AmazonCloudFormation, Unit] = {
    def inProgressError(ss: StackStatus) = raiseError(s"Can not update stack ${sd.name} as it has status ${ss.name}")
    stackStatus(sd.name).flatMap {
      case None | Some(DeleteComplete)     => create(sd)
      case Some(ss: InProgressStackStatus) => lift(inProgressError(ss))
      case Some(_)                         => update(sd)
    }
  }

  private def create(sd: StackDescription): Kleisli[IO, AmazonCloudFormation, Unit] =
    Kleisli { c =>
      val req = new CreateStackRequest()
        .withStackName(sd.name)
        .withTemplateBody(sd.templateBody)
        .withParameters(sd.cfnParams.asJava)
        .withTags(sd.cfnTags.asJava)
        .withCapabilities("CAPABILITY_IAM")
        .withCapabilities("CAPABILITY_NAMED_IAM")
      if (sd.policyBody.isDefined) req.setStackPolicyBody(sd.policyBody.get)
      IO(c.createStack(req))
    }

  private def update(sd: StackDescription): Kleisli[IO, AmazonCloudFormation, Unit] =
    Kleisli { c =>
      val req = new UpdateStackRequest()
        .withStackName(sd.name)
        .withTemplateBody(sd.templateBody)
        .withParameters(sd.cfnParams.asJava)
        .withTags(sd.cfnTags.asJava)
        .withCapabilities("CAPABILITY_IAM")
        .withCapabilities("CAPABILITY_NAMED_IAM")
      if (sd.policyBody.isDefined) req.setStackPolicyBody(sd.policyBody.get)
      IO(c.updateStack(req)).attempt.map {
        case Right(_) => ()
        case Left(e: AmazonCloudFormationException) if e.getMessage.startsWith("No updates are to be performed") => ()
        case Left(th) => throw th
      }
    }
}

