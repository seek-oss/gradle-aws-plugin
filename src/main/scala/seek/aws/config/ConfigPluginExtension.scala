package seek.aws
package config

import cats.effect.IO
import org.gradle.api.file.FileCollection
import org.gradle.api.{GradleException, Project}
import simulacrum.typeclass

import scala.collection.JavaConverters._
import scala.collection.mutable

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

  private[aws] var allowPropertyOverrides: Boolean = true
  def allowPropertyOverrides(v: Boolean): Unit = allowPropertyOverrides = v

  private[aws] var allowCommonConfig: Boolean = true
  def allowCommonConfig(v: Boolean): Unit = allowCommonConfig = v

  private[aws] var commonConfigName: String = "common"
  def commonConfigName(v: String): Unit = commonConfigName = v
}

object ConfigPluginExtension {
  private def buildConfigName(naming: String)(implicit p: Project): IO[String] = {
    def go(parts: List[String], acc: String = ""): IO[String] =
      parts match {
        case h :: t => resolve(h, naming).flatMap(v => go(t, acc + v + "."))
        case _      => IO.pure(acc.stripSuffix("."))
      }
    go(naming.split('.').toList)
  }

  private def resolve(key: String, naming: String)(implicit p: Project): IO[String] =
    lookupGradle(key).attempt.flatMap {
      case Right(v) => IO.pure(v)
      case Left(th) =>
        if (!th.isInstanceOf[LookupKeyNotFound]) IO.raiseError(th)
        else
          lookupParameterStore(key).attempt.map {
            case Right(v) => v
            case Left(th) =>
              if (!th.isInstanceOf[LookupKeyNotFound]) throw th
              else
                throw new GradleException(
                  s"Could not resolve property '${key}' in either Gradle project properties or AWS Parameter Store. " +
                    s"Property '${key}' is required to build config naming convention '${key}'.")
          }
    }

  private def lookupGradle(key: String)(implicit p: Project): IO[String] =
    IO(p.getProperties.asScala.get(key).map(_.asInstanceOf[String]) match {
      case None                 => throw new LookupKeyNotFound(key)
      case Some(v) if v.isEmpty => throw new LookupKeyNotFound(key)
      case Some(v)              => v
    })

  private def lookupParameterStore(key: String)(implicit p: Project): IO[String] =
    Lookup.parameterStore(key).run(p).attempt.map {
      case Left(th)              => throw th
      case Right(v) if v.isEmpty => throw new LookupKeyNotFound(key)
      case Right(v)              => v
    }
}

@typeclass trait HasConfigPluginExtension[A] {
  def cfgExt(a: A): ConfigPluginExtension
}
