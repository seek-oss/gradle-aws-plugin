package seek.aws
package cloudformation

import org.gradle.api.Project
import simulacrum.typeclass

import scala.collection.JavaConverters._

class CloudFormationPluginExtension(implicit project: Project) {
  import HasLazyProps._

  private[cloudformation] val stackName = lazyProp[String]("stackName")
  def stackName(v: Any): Unit = stackName.set(v)

  private[cloudformation] var parameters: Map[String, Any] = Map()
  def parameters(v: java.util.Map[String, Any]): Unit = parameters = v.asScala.toMap

  private[cloudformation] def resolvedParameters: Map[String, String] =
    parameters.mapValues { v =>
      val p = lazyProp[String]("")
      p.set(v)
      p.get
    }
}

@typeclass trait HasCloudFormationPluginExtension[A] {
  def cfnExt(a: A): CloudFormationPluginExtension
}

