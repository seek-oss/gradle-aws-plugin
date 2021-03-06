package seek.aws

import cats.effect.IO
import com.amazonaws.client.builder.AwsClientBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import seek.aws.instances._

abstract class AwsTask extends DefaultTask with HasLazyProperties {

  implicit val project = getProject

  setGroup("AWS")

  private var _region: Option[String] = None
  def region(v: String): Unit = _region = Option(v)

  private var _roleArn: Option[String] = None
  def roleArn(v: String): Unit = _roleArn = Option(v)

  protected val logger = getLogger

  @TaskAction
  def entryPoint(): Unit =
    run.unsafeRunSync()

  protected def run: IO[Unit]

  protected def buildClient[C](builder: AwsClientBuilder[_, C]): IO[C] =
    client.build(builder, region, roleArn)

  protected def region: Option[String] =
    _region.orElse(project.awsExt.region)

  protected  def roleArn: Option[String] =
    _roleArn.orElse(project.awsExt.roleArn)
}

