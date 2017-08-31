package seek.aws

import cats.effect.IO
import com.amazonaws.regions.Regions
import org.gradle.api.tasks.TaskAction
import org.gradle.api.{DefaultTask, InvalidUserCodeException}

import scala.util.{Failure, Success, Try}

abstract class AwsTask extends DefaultTask {

  setGroup("AWS")

  private val regionProp = LazyProp("region")
  def setRegion(v: Any) = regionProp.set(v)

  protected val logger = getLogger

  @TaskAction
  def entryPoint(): Unit =
    run.unsafeRunSync()

  protected def run: IO[Unit]

  protected def region: Regions =
    if (regionProp.isDefined)
      Regions.fromName(regionProp.get)
    else Try(getProject.getExtensions.getByType(classOf[AwsPluginExtension])) match {
      case Success(aws) => aws.region
      case Failure(_)   => throw new InvalidUserCodeException("AWS region not defined")
    }
}

