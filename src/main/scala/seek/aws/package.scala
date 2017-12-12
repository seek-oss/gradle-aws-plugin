package seek

import cats.data.Kleisli
import cats.effect.IO
import org.gradle.api.{GradleException, Project}
import simulacrum.typeclass

import scala.collection.JavaConverters._

package object aws {

  @typeclass trait HasGradleProperties[A] {
    def properties(a: A): Map[String, String]
  }

  object syntax extends
    HasGradleProperties.ToHasGradlePropertiesOps with
    HasAwsPluginExtension.ToHasAwsPluginExtensionOps

  object instances {
    implicit val projectHasGradleProperties = new HasGradleProperties[Project] {
      def properties(p: Project) =
        p.getProperties.asScala.mapValues(_.toString).toMap
    }

    implicit val projectHasAwsPluginExtension = new HasAwsPluginExtension[Project] {
      def awsExt(p: Project) =
        p.getExtensions.getByType(classOf[AwsPluginExtension])
    }
  }

  def raiseError(msg: String): IO[Unit] =
    IO.raiseError(new GradleException(msg))

  def maybeRun[C](
      shouldRun: LazyProperty[Boolean],
      runnable: Kleisli[IO, C, Boolean],
      onTrue: IO[Unit] = IO.unit,
      onFalse: IO[Unit] = IO.unit): Kleisli[IO, C, Unit] =
    Kleisli { c =>
      shouldRun.run.flatMap {
        case false => IO.unit
        case true  =>
          runnable.run(c).flatMap {
            case false => IO.unit
            case true  => onTrue
          }
      }
    }
}
