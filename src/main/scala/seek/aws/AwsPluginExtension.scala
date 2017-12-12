package seek.aws

import org.gradle.api.Project
import simulacrum.typeclass

class AwsPluginExtension(implicit project: Project) {
  import HasLazyProperties._

  type LookupIndex = Map[String, String]

  private[aws] val region = lazyProperty[String]("region", "ap-southeast-2")
  def region(v: Any): Unit = region.set(v)

  private[aws] val profile = lazyProperty[String]("profile", "default")
  def profile(v: Any): Unit = profile.set(v)
}

@typeclass trait HasAwsPluginExtension[A] {
  def awsExt(a: A): AwsPluginExtension
}
