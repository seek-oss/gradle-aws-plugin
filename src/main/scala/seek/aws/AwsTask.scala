package seek.aws

import cats.effect.IO
import com.amazonaws.client.builder.AwsClientBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import seek.aws.instances._
import seek.aws.syntax._

abstract class AwsTask extends DefaultTask with HasLazyProperties {

  implicit val project = getProject

  setGroup("AWS")

  private var region: Option[String] = None
  def region(v: String): Unit = region = Option(v)

  private var roleArn: Option[String] = None
  def roleArn(v: String): Unit = roleArn = Option(v)

  protected val logger = getLogger

  @TaskAction
  def entryPoint(): Unit =
    run.unsafeRunSync()

  protected def run: IO[Unit]

  protected def buildClient[C](builder: AwsClientBuilder[_, C]): IO[C] =
    client.build(builder, region.orElse(project.awsExt.region), roleArn.orElse(project.awsExt.roleArn))
}

