package seek.aws
package cloudformation

import java.io.File

import cats.data.Kleisli
import cats.data.Kleisli._
import cats.effect.IO
import com.amazonaws.services.cloudformation.model.{CreateStackRequest, Parameter, UpdateStackRequest}
import com.amazonaws.services.cloudformation.{AmazonCloudFormation, AmazonCloudFormationClientBuilder}
import org.apache.commons.codec.Charsets.UTF_8
import seek.aws.cloudformation.instances._
import seek.aws.cloudformation.syntax._

import scala.collection.JavaConverters._
import scala.io.Source._

class CreateOrUpdateStack extends AwsTask {

  setDescription("Creates a CloudFormation stack or updates it if it already exists")

  override def run: IO[Unit] =
    for {
      r  <- region
      c  <- IO.pure(AmazonCloudFormationClientBuilder.standard().withRegion(r).build())
      sn <- project.cfnExt.stackName.run
      ps <- project.cfnExt.resolvedParameters
      tf <- project.cfnExt.templateFile.runOptional
      tu <- project.cfnExt.templateUrl.runOptional
      pf <- project.cfnExt.policyFile.runOptional
      pu <- project.cfnExt.policyUrl.runOptional
      p  <- IO.pure(StackProperties(sn, ps, tf.map(slurp), tu, pf.map(slurp), pu))
      _  <- createOrUpdate(p).run(c)
      _  <- waitForStack(sn).run(c)
    } yield ()

  private def createOrUpdate(s: StackProperties): Kleisli[IO, AmazonCloudFormation, Unit] = {
    def inProgressError(ss: StackStatus) = raiseError(s"Can not update stack ${s.name} as it has status ${ss.name}")
    stackStatus(s.name).flatMap {
      case None | Some(DeleteComplete)     => create(s)
      case Some(ss: InProgressStackStatus) => lift(inProgressError(ss))
      case Some(_)                         => update(s)
    }
  }

  private def create(s: StackProperties): Kleisli[IO, AmazonCloudFormation, Unit] =
    Kleisli { c =>
      val req = new CreateStackRequest()
        .withStackName(s.name)
        .withParameters(s.cfnParams.asJava)
        .withCapabilities("CAPABILITY_IAM")
        .withCapabilities("CAPABILITY_NAMED_IAM")
      for {
        _ <- checkTemplateArgs(s)
        _ <- checkPolicyArgs(s)
        r <- IO.pure(req)
        _ = if (s.templateBody.isDefined) r.setTemplateBody(s.templateBody.get)
        _ = if (s.templateUrl.isDefined) r.setTemplateURL(s.templateUrl.get)
        _ = if (s.policyBody.isDefined) r.setStackPolicyBody(s.policyBody.get)
        _ = if (s.policyUrl.isDefined) r.setStackPolicyBody(s.policyUrl.get)
        _ <- IO(c.createStack(r))
      } yield ()
    }

  private def update(s: StackProperties): Kleisli[IO, AmazonCloudFormation, Unit] =
    Kleisli { c =>
      val req = new UpdateStackRequest()
        .withStackName(s.name)
        .withParameters(s.cfnParams.asJava)
        .withCapabilities("CAPABILITY_IAM")
        .withCapabilities("CAPABILITY_NAMED_IAM")
      for {
        _ <- checkTemplateArgs(s)
        _ <- checkPolicyArgs(s)
        r <- IO.pure(req)
        _ = if (s.templateBody.isDefined) r.setTemplateBody(s.templateBody.get)
        _ = if (s.templateUrl.isDefined) r.setTemplateURL(s.templateUrl.get)
        _ = if (s.policyBody.isDefined) r.setStackPolicyBody(s.policyBody.get)
        _ = if (s.policyUrl.isDefined) r.setStackPolicyBody(s.policyUrl.get)
        _ <- IO(c.updateStack(r))
      } yield ()
    }

  private def slurp(f: File): String =
    fromFile(f, UTF_8.name()).mkString

  private def checkTemplateArgs(s: StackProperties): IO[Unit] =
    (s.templateBody, s.templateUrl) match {
      case (Some(_), None) => IO.unit
      case (None, Some(_)) => IO.unit
      case _ => raiseError("Either templateFile or templateUrl must be set but not both")
    }

  private def checkPolicyArgs(s: StackProperties): IO[Unit] =
    (s.policyBody, s.policyUrl) match {
      case (Some(_), Some(_)) => raiseError("Either policyFile or policyUrl can be set but not both")
      case _                  => IO.unit
    }

  private case class StackProperties(
    name: String,
    parameters: Map[String, String],
    templateBody: Option[String],
    templateUrl: Option[String],
    policyBody: Option[String],
    policyUrl: Option[String]) {

    def cfnParams: List[Parameter] =
      parameters.toList.map {
        case (k, v) => new Parameter().withParameterKey(k).withParameterValue(v)
      }
  }
}
