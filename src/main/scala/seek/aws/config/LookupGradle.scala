package seek.aws.config

import cats.data.OptionT
import cats.data.OptionT._
import cats.effect.IO
import org.gradle.api.Project
import seek.aws.config.instances._
import seek.aws.config.syntax._

case class LookupGradle(key: String) extends Lookup {
  private val reservedKeywords = List("project")

  def runOptional(p: Project): OptionT[IO, String] =
    if (hasProperty(p, key)) liftF(IO(p.property(key).toString))
    else none

  private def hasProperty(p: Project, key: String): Boolean =
    p.cfgExt.allowPropertyOverrides && !reservedKeywords.contains(key) && p.hasProperty(key)
}
