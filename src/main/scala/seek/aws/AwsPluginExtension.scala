package seek.aws

import org.gradle.api.Project

class AwsPluginExtension(implicit project: Project) {
  private[aws] var region: Option[String] = None
  def region(v: String): Unit = region = Option(v)

  private[aws] var roleArn: Option[String] = None
  def roleArn(v: String): Unit = roleArn = Option(v)
}
