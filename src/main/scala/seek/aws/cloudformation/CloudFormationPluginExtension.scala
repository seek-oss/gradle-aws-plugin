package seek.aws
package cloudformation

import java.io.File

import cats.effect.IO
import org.gradle.api.Project
import seek.aws.LazyProperty._
import seek.aws.config.Lookup
import simulacrum.typeclass

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, _}

class CloudFormationPluginExtension(implicit project: Project) {
  import HasLazyProperties._

  private[cloudformation] val stackName = lazyProperty[String]("stackName")
  def stackName(v: Any): Unit = stackName.set(v)

  private[cloudformation] val templateFile = lazyProperty[File]("templateFile")
  def templateFile(v: Any): Unit = templateFile.set(v)

  private[cloudformation] val policyFile = lazyProperty[File]("policyFile")
  def policyFile(v: Any): Unit = policyFile.set(v)

  private var _parameters: Map[String, Any] = Map.empty
  def parameters(v: java.util.Map[String, Any]): Unit = _parameters = v.asScala.toMap

  private var _tags: Map[String, Any] = Map.empty
  def tags(v: java.util.Map[String, Any]): Unit = _tags = v.asScala.toMap

  private var lookupTags: List[String] = List.empty
  def tags(v: java.util.List[String]): Unit = lookupTags = v.asScala.toList

  private[cloudformation] def parameters: IO[Map[String, String]] =
    renderValues(_parameters)

  private[cloudformation] def tags: IO[Map[String, String]] =
    renderValues[String, String](_tags).flatMap { ts =>
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

