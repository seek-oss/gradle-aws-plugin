package seek.aws

import cats.effect.IO
import com.amazonaws.regions.Regions
import com.amazonaws.regions.Regions.fromName
import org.gradle.api.tasks.TaskAction
import org.gradle.api.{DefaultTask, GradleException}
import seek.aws.instances._
import seek.aws.syntax._

abstract class AwsTask extends DefaultTask with HasLazyProps {

  implicit val project = getProject

  setGroup("AWS")

  private val _region = lazyProp[String]("region")
  def region(v: Any) = _region.set(v)

  protected val logger = getLogger

  @TaskAction
  def entryPoint(): Unit =
    run.unsafeRunSync()

  protected def run: IO[Unit]

  protected def region: IO[Regions] =
    (if (_region.isSet) _region.run
    else project.awsExt.region.run).map(fromName)

  protected def raiseError(msg: String): IO[Unit] =
    IO.raiseError(new GradleException(msg))
}

