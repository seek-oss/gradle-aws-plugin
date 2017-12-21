package seek.aws

import org.gradle.api.Project
import simulacrum.typeclass

class AwsPluginExtension(implicit project: Project) {
  import HasLazyProperties._

  private[aws] val region = lazyProperty[String]("region")
  def region(v: Any): Unit = region.set(v)

  private[aws] val profile = lazyProperty[String]("profile", "default")
  def profile(v: Any): Unit = profile.set(v)

  private[aws] val roleArn = lazyProperty[String]("roleArn")
  def roleArn(v: Any): Unit = roleArn.set(v)
}

@typeclass trait HasAwsPluginExtension[A] {
  def awsExt(a: A): AwsPluginExtension
}
