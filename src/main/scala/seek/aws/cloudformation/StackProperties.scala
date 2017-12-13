package seek.aws.cloudformation

import java.io.File

import cats.effect.IO
import com.amazonaws.services.cloudformation.model.{Parameter, Tag}
import org.apache.commons.codec.Charsets.UTF_8
import org.gradle.api.Project
import seek.aws.cloudformation.CloudFormationTemplate.parseTemplateParameters
import seek.aws.cloudformation.instances._
import seek.aws.cloudformation.syntax._
import seek.aws.config.{LookupProject, LookupProjectFailed}
import seek.aws.pascalToCamelCase

import scala.io.Source.fromFile

case class StackProperties(
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

object StackProperties {

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
      LookupProject.lookup(project, pascalToCamelCase(p.name), parameterOverrides).attempt.flatMap {
        case Right(v) => z.map(_ + (p.name -> v))
        case Left(th) =>
          if (!p.required && th.isInstanceOf[LookupProjectFailed]) z else IO.raiseError(th)
      }
    })

  private def slurp(f: File): IO[String] =
    IO(fromFile(f, UTF_8.name()).mkString)

  private def maybeSlurp(f: Option[File]): IO[Option[String]] =
    f match {
      case None    => IO.pure(None)
      case Some(f) => slurp(f).map(Some(_))
    }
}
