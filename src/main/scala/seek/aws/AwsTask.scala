package seek.aws

import cats.effect.IO
import com.amazonaws.regions.Regions
import org.gradle.api.{DefaultTask, InvalidUserCodeException}
import org.gradle.api.tasks.TaskAction

abstract class AwsTask extends DefaultTask {

  setGroup("AWS")

  private val regionProp = lazyProp[String]("region")
  def setRegion(v: Any) = regionProp.set(v)

  protected val logger = getLogger

  @TaskAction
  def entryPoint(): Unit =
    run.unsafeRunSync()

  protected def run: IO[Unit]

  protected def region: Regions =
    regionProp.getOption match {
      case Some(r) => Regions.fromName(r)
      case None    => awsPluginExtension.region
    }

  protected def lazyProp[A](name: String): LazyProp[A] =
    LazyProp[A](name, awsPluginExtension.lookupPrefix, getProject.getProperties)

  protected def lazyProp[A](name: String, default: A): LazyProp[A] =
    LazyProp[A](name, awsPluginExtension.lookupPrefix, getProject.getProperties, Some(default))

  protected def userCodeErrorIO(msg: String): IO[Unit] =
    IO.raiseError(new InvalidUserCodeException(msg))

  private def awsPluginExtension: AwsPluginExtension =
    getProject.getExtensions.getByType(classOf[AwsPluginExtension])
}

