package seek.aws
package config

import cats.data.OptionT
import cats.data.OptionT._
import cats.effect.IO
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import simulacrum.typeclass

import scala.collection.mutable

class ConfigPluginExtension(implicit project: Project) {
  import ConfigPluginExtension._

  private var naming: String = "environment"
  def naming(v: String): Unit = naming = v
  private[aws] def configName: IO[String] = buildConfigName(naming)

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
    notEmpty(LookupGradle(key).runOptional(p))
      .orElse(notEmpty(LookupParameterStore(key).runOptional(p)))
      .value
      .flatMap {
        case Some(v) => IO.pure(v)
        case None    => raiseError(
          s"Could not resolve property '${key}' in either Gradle project properties or AWS Parameter Store. " +
          s"Property '${key}' is required to build config naming convention '${naming}'.")
      }

  private def notEmpty(o: OptionT[IO, String]): OptionT[IO, String] =
    o.flatMap(s => if (s == null || s.isEmpty) none else some(s))
}

@typeclass trait HasConfigPluginExtension[A] {
  def cfgExt(a: A): ConfigPluginExtension
}
