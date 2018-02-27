package seek.aws
package cloudformation

import java.io.File

import cats.effect.IO
import org.gradle.api.Project
import seek.aws.LazyProperty._
import seek.aws.config.Lookup
import simulacrum.typeclass

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration.{Duration, _}

class CloudFormationPluginExtension(implicit project: Project) {
  import HasLazyProperties._

  private[cloudformation] val stackName = lazyProperty[String]("stackName")
  def stackName(v: Any): Unit = stackName.set(v)

  private[cloudformation] val templateFile = lazyProperty[File]("templateFile")
  def templateFile(v: Any): Unit = templateFile.set(v)

  private[cloudformation] val policyFile = lazyProperty[File]("policyFile")
  def policyFile(v: Any): Unit = policyFile.set(v)

  private val _parameters = mutable.HashMap.empty[String, Any]
  def parameters(v: java.util.Map[String, Any]): Unit = {
    _parameters.clear
    _parameters ++= v.asScala
  }
  def addParameters(v: java.util.Map[String, Any]): Unit = _parameters ++= v.asScala

  private val _tags = mutable.HashMap.empty[String, Any]
  def tags(v: java.util.Map[String, Any]): Unit = {
    _tags.clear
    _tags ++= v.asScala
  }
  def addTags(v: java.util.Map[String, Any]): Unit = _tags ++= v.asScala

  private val lookupTags = mutable.Set.empty[String]
  def tags(v: java.util.List[String]): Unit = {
    lookupTags.clear
    lookupTags ++= v.asScala
  }
  def addTags(v: java.util.List[String]): Unit = lookupTags ++= v.asScala

  private[cloudformation] def parameters: IO[Map[String, String]] =
    renderValues[String, String](_parameters.toMap)

  private[cloudformation] def tags: IO[Map[String, String]] =
    renderValues[String, String](_tags.toMap).flatMap { ts =>
      lookupTags.foldLeft(IO.pure(ts)) { (z, t) =>
        for {
          zz <- z
          tv <- Lookup.lookup(pascalToCamelCase(t)).run(project)
        } yield zz + (t -> tv)
      }
    }

  private val stackWaitTimeoutSeconds = lazyProperty[Any]("stackWaitTimeoutSeconds", 900)
  def stackWaitTimeoutSeconds(v: Any): Unit = stackWaitTimeoutSeconds.set(v)
  private[cloudformation] def stackWaitTimeout: IO[Duration] =
    stackWaitTimeoutSeconds.run.map(_.toString.toInt.seconds)
}

@typeclass trait HasCloudFormationPluginExtension[A] {
  def cfnExt(a: A): CloudFormationPluginExtension
}

