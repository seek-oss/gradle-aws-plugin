package seek.aws

import cats.effect.IO
import com.amazonaws.auth._
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
    for {
      r <- region.or(project.awsExt.region).run
      c <- credentials
      _ = builder.setRegion(r)
      _ = builder.setCredentials(c)
    } yield builder.build()

  private def credentials: IO[AWSCredentialsProvider] =
    roleArn.or(project.awsExt.roleArn).runOptional.map {
      case None      => DefaultAWSCredentialsProviderChain.getInstance
      case Some(arn) =>
        new STSAssumeRoleSessionCredentialsProvider.Builder(arn, project.getName)
          .withRoleSessionDurationSeconds(900)
          .build()
    }
}

