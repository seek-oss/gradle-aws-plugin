package seek

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.auth._
import com.amazonaws.client.builder.AwsClientBuilder
import org.gradle.api.file.{FileCollection, FileTreeElement}
import org.gradle.api.{GradleException, Project}
import pureconfig._

import scala.collection.mutable

package object aws {

  object instances {
    implicit class ProjectHasAwsPluginExtension(p: Project) {
      def awsExt =
        p.getExtensions.getByType(classOf[AwsPluginExtension])
    }
  }

  object client {
    def build[C](builder: AwsClientBuilder[_, C], region: Option[String], roleArn: Option[String]): IO[C] = {
      if (region.isDefined) builder.setRegion(region.get)
      builder.setCredentials(credentials(roleArn))
      IO(builder.build())
    }

    def credentials(roleArn: Option[String]): AWSCredentialsProvider =
      roleArn match {
        case Some(arn) if arn.nonEmpty =>
          new STSAssumeRoleSessionCredentialsProvider.Builder(arn, "Gradle")
            .withRoleSessionDurationSeconds(900)
            .build()
        case _ => DefaultAWSCredentialsProviderChain.getInstance
      }
  }

  def raiseError[A](msg: String): IO[A] =
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

  def gather[A](ios: Seq[IO[A]]): IO[List[A]] =
    ios.foldRight(IO.pure(List.empty[A]))((p, z) => z.flatMap(zz => p.map(_ :: zz)))

  def pascalToCamelCase(s: String): String =
    ConfigFieldMapping(PascalCase, CamelCase).apply(s)

  def camelToKebabCase(s: String): String =
    ConfigFieldMapping(CamelCase, KebabCase).apply(s)

  def camelToSnakeCase(s: String): String =
    ConfigFieldMapping(CamelCase, SnakeCase).apply(s)

  def camelToDotCase(s: String): String =
    ConfigFieldMapping(CamelCase, new StringDelimitedNamingConvention(".")).apply(s)

  def fileTreeElements(fc: FileCollection): List[FileTreeElement] = {
    val buf = new mutable.ArrayBuffer[FileTreeElement]()
    fc.getAsFileTree.visit(d => buf += d)
    buf.filter(e => e.getFile.isFile && e.getFile.exists).toList
  }
}
