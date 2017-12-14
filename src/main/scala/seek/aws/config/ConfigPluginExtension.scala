package seek.aws.config

import cats.effect.IO
import org.gradle.api.{GradleException, Project}
import org.gradle.api.file.FileCollection
import simulacrum.typeclass

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.JavaConverters._

class ConfigPluginExtension(implicit project: Project) {
  import ConfigPluginExtension._

  private var naming: String = "environment"
  def naming(v: String): Unit = naming = v
  def configName: IO[String] = buildConfigName(naming)

  private[aws] val files = mutable.ArrayBuffer.empty[FileCollection]
  def addFiles(fs: FileCollection): Unit = files += fs
  def files(fs: FileCollection): Unit = {
    files.clear()
    addFiles(fs)
  }

  private[aws] var allowProjectOverrides: Boolean = true
  def allowProjectOverrides(v: Boolean): Unit = allowProjectOverrides = v

  private[aws] var allowCommonConfig: Boolean = true
  def allowCommonConfig(v: Boolean): Unit = allowCommonConfig = v

  private[aws] var commonConfigName: String = "common"
  def commonConfigName(v: String): Unit = commonConfigName = v
}

object ConfigPluginExtension {
  private def buildConfigName(naming: String)(implicit p: Project): IO[String] = {
    @tailrec
    def go(parts: List[String], acc: IO[String] = IO.pure("")): String =
      parts match {
        case h :: t =>
          lookupGradle(h).map {
            case Some(v) => v
            case None    =>
              lookupParameterStore(h).map {
                case Some(v) => v
                case None    => throwMissingKeyError(h, naming)
              }
          }
          lookupGradle(h).map(_.o)
          p.getProperties.asScala.get(h).map(_.asInstanceOf[String]) match {
            case Some(v: String) if v.nonEmpty => go(t, acc + v + ".")
            case _ => throw new GradleException(
              s"Project property with name '${h}' is required to build lookup index ${naming}")
          }
        case _ => acc.stripSuffix(".")
      }

    go(naming.split('.').toList)
  }

  private def lookupGradle(key: String)(implicit p: Project): IO[Option[String]] =
    IO(p.getProperties.asScala.get(key).map(_.asInstanceOf[String]).flatMap(nonEmpty))

  private def lookupParameterStore(key: String)(implicit p: Project): IO[Option[String]] =
    Lookup.parameterStore(key).run(p).attempt.map {
      case Left(_: LookupKeyNotFound) => None
      case Left(th)                   => throw th
      case Right(v)                   => Some(v).flatMap(nonEmpty)
    }

  private def nonEmpty(s: String): Option[String] =
    Option(s).flatMap(s => if (s.nonEmpty) Some(s) else None)

  private def throwMissingKeyError(key: String, naming: String) =
    throw new GradleException(
      s"Could not resolve property '${key}' in either Gradle project properties or AWS Parameter Store. " +
       "Property '${key}' is required to build config naming convention '${key}'.")
}

@typeclass trait HasConfigPluginExtension[A] {
  def cfgExt(a: A): ConfigPluginExtension
}
