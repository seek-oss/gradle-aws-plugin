package seek.aws

import org.gradle.api.Project
import simulacrum.typeclass

class AwsPluginExtension(implicit project: Project) {
  import HasLazyProps._

  private[aws] val region = lazyProp[String]("region", "ap-southeast-2")
  def region(v: Any): Unit = region.set(v)

  private[aws] val profile = lazyProp[String]("profile", "default")
  def profile(v: Any): Unit = profile.set(v)

  private[aws] var lookupPrefix: String = "environment"
  def lookupPrefix(v: String): Unit = lookupPrefix = v
}

@typeclass trait HasAwsPluginExtension[A] {
  def awsExt(a: A): AwsPluginExtension
}
