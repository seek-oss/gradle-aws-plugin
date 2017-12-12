package seek.aws
package cloudformation

import java.io.File

import cats.data.Kleisli
import cats.data.Kleisli._
import cats.effect.IO
import com.amazonaws.services.cloudformation.model.{CreateStackRequest, Parameter, Tag, UpdateStackRequest}
import com.amazonaws.services.cloudformation.{AmazonCloudFormation, AmazonCloudFormationClientBuilder}
import org.apache.commons.codec.Charsets.UTF_8
import org.gradle.api.Project
import pureconfig.{CamelCase, ConfigFieldMapping, PascalCase}
import seek.aws.cloudformation.CloudFormationTemplate._
import seek.aws.cloudformation.instances._
import seek.aws.cloudformation.syntax._
import seek.aws.lookup.ProjectLookup

import scala.collection.JavaConverters._
import scala.io.Source._

class CreateOrUpdateStack extends AwsTask {

  setDescription("Creates a CloudFormation stack or updates it if it already exists")

  override def run: IO[Unit] =
    for {
      r  <- region
      c  <- IO.pure(AmazonCloudFormationClientBuilder.standard().withRegion(r).build())
      sp <- StackProperties(project)
      _  <- createOrUpdate(sp).run(c)
      _  <- waitForStack(sp.name).run(c)
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
        .withTemplateBody(s.templateBody)
        .withParameters(s.cfnParams.asJava)
        .withTags(s.cfnTags.asJava)
        .withCapabilities("CAPABILITY_IAM")
        .withCapabilities("CAPABILITY_NAMED_IAM")
      if (s.policyBody.isDefined) req.setStackPolicyBody(s.policyBody.get)
      IO(c.createStack(req))
    }

  private def update(s: StackProperties): Kleisli[IO, AmazonCloudFormation, Unit] =
    Kleisli { c =>
      val req = new UpdateStackRequest()
        .withStackName(s.name)
        .withTemplateBody(s.templateBody)
        .withParameters(s.cfnParams.asJava)
        .withTags(s.cfnTags.asJava)
        .withCapabilities("CAPABILITY_IAM")
        .withCapabilities("CAPABILITY_NAMED_IAM")
      if (s.policyBody.isDefined) req.setStackPolicyBody(s.policyBody.get)
      IO(c.updateStack(req))
    }

  private case class StackProperties(
      name: String,
      templateBody: String,
      policyBody: Option[String],
      parameters: Map[String, String],
      tags: Map[String, String]) {

    def cfnParams: List[Parameter] =
      parameters.toList.map {
        case (k, v) => new Parameter().withParameterKey(k).withParameterValue(v)
      }

    def cfnTags: List[Tag] =
      tags.toList.map {
        case (k, v) => new Tag().withKey(k).withValue(v)
      }
  }

  private object StackProperties {

    def apply(project: Project): IO[StackProperties] =
      for {
        sn <- project.cfnExt.stackName.run
        tf <- project.cfnExt.templateFile.run
        pf <- project.cfnExt.policyFile.runOptional
        ps <- project.cfnExt.parameters
        ts <- project.cfnExt.tags
        tb <- slurp(tf)
        pb <- maybeSlurp(pf)
        ps <- resolveStackParameters(project, tf, ps)
      } yield StackProperties(sn, tb, pb, ps, ts)

    private def resolveStackParameters(
        project: Project, templateFile: File, parameterOverrides: Map[String, String]): IO[Map[String, String]] =
      parseTemplateParameters(templateFile).flatMap(_.foldLeft(IO.pure(Map.empty[String, String])) { (z, p) =>
        val pv = ProjectLookup.lookup(project, pascalToCamelCase(p.name), parameterOverrides)
        z.flatMap(m => pv.map(v => m + (p.name -> v)))
      })

    private def slurp(f: File): IO[String] =
      IO(fromFile(f, UTF_8.name()).mkString)

    private def maybeSlurp(f: Option[File]): IO[Option[String]] =
      f match {
        case None    => IO.pure(None)
        case Some(f) => slurp(f).map(Some(_))
      }

  }
}

