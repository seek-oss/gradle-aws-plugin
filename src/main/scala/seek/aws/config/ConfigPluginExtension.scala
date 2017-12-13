package seek.aws.config

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import simulacrum.typeclass

import scala.collection.mutable

class ConfigPluginExtension(implicit project: Project) {

  private[aws] var index: String = "environment"
  def index(v: String): Unit = index = v

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

@typeclass trait HasConfigPluginExtension[A] {
  def cfgExt(a: A): ConfigPluginExtension
}
