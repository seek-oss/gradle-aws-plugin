package seek

import cats.data.Kleisli
import cats.effect.IO
import org.gradle.api.Project

package object aws {

  object syntax extends HasAwsPluginExtension.ToHasAwsPluginExtensionOps

  object instances {
    implicit val projectHasAwsPluginExtension = new HasAwsPluginExtension[Project] {
      def awsExt(p: Project) =
        p.getExtensions.getByType(classOf[AwsPluginExtension])
    }
  }

  def maybeRun[C](
      shouldRun: LazyProp[Boolean],
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
