package seek.aws
package cloudformation

import java.io.File

import cats.effect.IO
import org.gradle.api.Project
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

  private[cloudformation] var parameters: Map[String, Any] = Map()
  def parameters(v: java.util.Map[String, Any]): Unit = parameters = v.asScala.toMap

  private[cloudformation] var tags: Map[String, Any] = Map()
  def tags(v: java.util.Map[String, Any]): Unit = tags = v.asScala.toMap

  private[cloudformation] def resolvedParameters: IO[Map[String, String]] =
    resolveMap(parameters)

  private[cloudformation] def resolvedTags: IO[Map[String, String]] =
    resolveMap(tags)

  private def resolveMap(m: Map[String, Any]): IO[Map[String, String]] =
    m.foldLeft(IO.pure(Map.empty[String, String])) {
      case (z, (k, v)) =>
        val p = lazyProp[String]("")
        p.set(v)
        for {
          m <- z
          x <- p.run
        } yield m + (k -> x)
    }
}

@typeclass trait HasCloudFormationPluginExtension[A] {
  def cfnExt(a: A): CloudFormationPluginExtension
}
