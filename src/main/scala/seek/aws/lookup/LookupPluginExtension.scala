package seek.aws.lookup

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import simulacrum.typeclass

import scala.collection.mutable

class LookupPluginExtension(implicit project: Project) {

  private[aws] var key: String = "environment"
  def key(v: String): Unit = key = v

  private[aws] val files = mutable.ArrayBuffer.empty[FileCollection]
  def addFiles(fs: FileCollection): Unit = files += fs

  private[aws] var allowProjectOverrides: Boolean = true
  def allowProjectOverrides(v: Boolean): Unit = allowProjectOverrides = v
}

@typeclass trait HasLookupPluginExtension[A] {
  def lookupExt(a: A): LookupPluginExtension
}
