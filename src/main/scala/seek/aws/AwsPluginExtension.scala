package seek.aws

import org.gradle.api.Project
import simulacrum.typeclass

class AwsPluginExtension(implicit project: Project) {

  private[aws] var region: Option[String] = None
  def region(v: String): Unit = region = Option(v)

  private[aws] var roleArn: Option[String] = None
  def roleArn(v: String): Unit = {
    println(s"Setting roleArn to ${v}")
    roleArn = Option(v)
  }
}

@typeclass trait HasAwsPluginExtension[A] {
  def awsExt(a: A): AwsPluginExtension
}
