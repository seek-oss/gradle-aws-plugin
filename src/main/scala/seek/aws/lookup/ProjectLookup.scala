package seek.aws
package lookup

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}
import org.gradle.api.{GradleException, Project}
import seek.aws.lookup.instances._
import seek.aws.lookup.syntax._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable

class ProjectLookup(key: String) extends Lookup {

  def run(p: Project): IO[String] =
    ProjectLookup.lookup(p, key)
}

class ProjectLookupException(key: String) extends GradleException(
  s"Could not find a project key with name '${key}'")

object ProjectLookup {

  // Impure
  private val cache = mutable.Map.empty[Project, Config]

  def lookup(p: Project, key: String): IO[String] =
    findOverride(p, key) match {
      case Some(v) => IO.pure(v)
      case None    =>
        cache.get(p) match {
          case Some(c) => IO(c.getString(key))
          case None    => buildCache(p).flatMap(_ => lookup(p, key))
        }
    }

  def lookup2(p: Project, key: String, overrides: Map[String, String] = Map.empty): IO[String] =
    if (p.lookupExt.allowProjectOverrides && p.hasProperty(key)) Some(p.property(key).toString)

  private def findOverride(p: Project, key: String): Option[String] =
    if (p.lookupExt.allowProjectOverrides && p.hasProperty(key)) Some(p.property(key).toString)
    else None

  private def buildCache(p: Project): IO[Unit] =
    IO(cache.put(p, buildConfig(p)))

  private def buildConfig(p: Project): Config = {
    @tailrec
    def go(parts: List[String], acc: String = ""): String =
      parts match {
        case h :: t =>
          p.getProperties.asScala.get(h).map(_.asInstanceOf[String]) match {
            case Some(v: String) if v.nonEmpty => go(t, acc + v + ".")
            case _ => throw new ProjectLookupException(h)
          }
        case _ => acc
      }
    val fname = go(p.lookupExt.key.split(".").toList)
    val matching = p.lookupExt.files.reverse.map(_.getFiles.asScala.filter(_.getName == s"${fname}.conf"))
    val configs = matching.toList.flatMap(_.toList).map(f => ConfigFactory.parseFile(f))
    configs.foldLeft(ConfigFactory.empty())((z, c) => z.withFallback(c))
  }
}

// TODO: Make this class utilise the lookupIndex as described in the aws plugin extension.
// It will need to cache by project after it creates an index for each project it's given on run
// It will need to read in all the files in  the lookupFiles and parse them out normalising the casing to camelCase

