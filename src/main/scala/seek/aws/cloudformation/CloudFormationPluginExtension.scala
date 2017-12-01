package seek.aws
package cloudformation

import java.io.File

import cats.effect.IO
import org.gradle.api.Project
import seek.aws.LazyProp._
import simulacrum.typeclass

import scala.collection.JavaConverters._

class CloudFormationPluginExtension(implicit project: Project) {
  import HasLazyProps._

  private[cloudformation] val stackName = lazyProp[String]("stackName")
  def stackName(v: Any): Unit = stackName.set(v)

  private[cloudformation] val templateFile = lazyProp[File]("templateFile")
  def templateFile(v: Any): Unit = templateFile.set(v)

  private[cloudformation] val templateUrl = lazyProp[String]("templateUrl")
  def templateUrl(v: Any): Unit = templateUrl.set(v)

  private[cloudformation] val policyFile = lazyProp[File]("policyFile")
  def policyFile(v: Any): Unit = policyFile.set(v)

  private[cloudformation] val policyUrl = lazyProp[String]("policyUrl")
  def policyUrl(v: Any): Unit = policyUrl.set(v)

  private var _parameters: Map[String, Any] = Map()
  def parameters(v: java.util.Map[String, Any]): Unit = _parameters = v.asScala.toMap

  private var _tags: Map[String, Any] = Map()
  def tags(v: java.util.Map[String, Any]): Unit = _tags = v.asScala.toMap

  private[cloudformation] def parameters: IO[Map[String, String]] =
    renderValues(_parameters)

  private[cloudformation] def tags: IO[Map[String, String]] =
    renderValues(_tags)
}

@typeclass trait HasCloudFormationPluginExtension[A] {
  def cfnExt(a: A): CloudFormationPluginExtension
}

