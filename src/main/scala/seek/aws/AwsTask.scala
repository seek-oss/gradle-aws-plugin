package seek.aws

import cats.effect.IO
import com.amazonaws.regions.Regions
import org.gradle.api.tasks.TaskAction
import org.gradle.api.{DefaultTask, InvalidUserCodeException}
import seek.aws.syntax._
import seek.aws.instances._

abstract class AwsTask extends DefaultTask with HasLazyProps {

  implicit val project = getProject

  setGroup("AWS")

  private val regionProp = lazyProp[String]("region")
  def region(v: Any) = regionProp.set(v)

  protected val logger = getLogger

  @TaskAction
  def entryPoint(): Unit =
    run.unsafeRunSync()

  protected def run: IO[Unit]

  protected def region: Regions =
    regionProp.getOption match {
      case Some(r) => Regions.fromName(r)
      case None    => Regions.fromName(project.awsExt.region.get)
    }

  protected def raiseUserCodeError(msg: String): IO[Unit] =
    IO.raiseError(new InvalidUserCodeException(msg))
}

