package seek.aws

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import simulacrum.typeclass

import scala.collection.mutable

class AwsPluginExtension(implicit project: Project) {
  import HasLazyProperties._

  type LookupIndex = Map[String, String]

  private[aws] val region = lazyProperty[String]("region", "ap-southeast-2")
  def region(v: Any): Unit = region.set(v)

  private[aws] val profile = lazyProperty[String]("profile", "default")
  def profile(v: Any): Unit = profile.set(v)

  private[aws] var lookupBy: String = "environment"
  def lookupBy(v: String): Unit = lookupBy = v

  private[aws] val lookupFiles = mutable.ArrayBuffer.empty[FileCollection]
  def addLookupFiles(fs: FileCollection): Unit = lookupFiles += fs
}

@typeclass trait HasAwsPluginExtension[A] {
  def awsExt(a: A): AwsPluginExtension
}
