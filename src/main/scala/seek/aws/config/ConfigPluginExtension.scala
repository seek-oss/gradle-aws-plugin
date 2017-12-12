package seek.aws.config

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import simulacrum.typeclass

import scala.collection.mutable

class ConfigPluginExtension(implicit project: Project) {

  private[aws] var lookupBy: String = "environment"
  def lookupBy(v: String): Unit = lookupBy = v

  private[aws] val files = mutable.ArrayBuffer.empty[FileCollection]
  def addFiles(fs: FileCollection): Unit = files += fs
  def files(fs: FileCollection): Unit = {
    files.clear()
    addFiles(fs)
  }

  private[aws] var allowProjectOverrides: Boolean = true
  def allowProjectOverrides(v: Boolean): Unit = allowProjectOverrides = v
}

@typeclass trait HasConfigPluginExtension[A] {
  def configExt(a: A): ConfigPluginExtension
}
