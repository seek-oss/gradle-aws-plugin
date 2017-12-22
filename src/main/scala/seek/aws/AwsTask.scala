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

  private val region = lazyProperty[String]("region")
  def region(v: Any): Unit = region.set(v)

  private val roleArn = lazyProperty[String]("roleArn")
  def roleArn(v: Any): Unit = roleArn.set(v)

  protected val logger = getLogger

  @TaskAction
  def entryPoint(): Unit =
    run.unsafeRunSync()

  protected def run: IO[Unit]

  protected def buildClient[C](builder: AwsClientBuilder[_, C]): IO[C] =
    client.build(builder, region.orElse(project.awsExt.region), roleArn.orElse(project.awsExt.roleArn))
}

