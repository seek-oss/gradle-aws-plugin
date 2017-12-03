package seek.aws

import cats.effect.IO
import org.gradle.api.{GradleException, Project}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import syntax._
import instances._
import seek.aws.ProjectLookup.property

class ProjectLookup(key: String) extends Lookup {

  def run(p: Project): IO[String] =
    ProjectLookup.lookup(p, key)
}

class ProjectLookupException(key: String) extends GradleException(
  s"Could not find a project key with name '${key}'")

object ProjectLookup {

  private val cache = mutable.Map.empty[Project, Map[String, String]]

  def lookup(p: Project, key: String): IO[String] =
    IO(property(p, key)).map {
      case Some(v) => v
      case None    =>
        val fullKey = buildKey(p, key, lookupBy(p).split('.').toList)
        property(p, fullKey).getOrElse(throw new ProjectLookupException(fullKey))
    }

  @tailrec
  private def buildKey(p: Project, key: String, prefixParts: List[String], acc: String = ""): String =
    prefixParts match {
      case h :: t =>
        property(p, h) match {
          case Some(v: String) if v.nonEmpty => buildKey(p, key, t, acc + v + ".")
          case _ => throw new ProjectLookupException(h)
        }
      case _ => acc + key
    }

  private def property(p: Project, key: String): Option[String] =
    p.getProperties.asScala.get(key) match {
      case Some(v) => Some(v.asInstanceOf[String])
      case _       => None
    }

  private def lookupBy(p: Project): String =
    p.getExtensions.getByType(classOf[AwsPluginExtension]).lookupBy
}

object ProjectLookup2 {

  private val cache = mutable.Map.empty[Project, Map[String, String]]

  def lookup(p: Project, key: String): IO[String] =
    cache.get(p) match {
      case Some(m) => IO(m.getOrElse(key, throw new ProjectLookupException(key)))
      case None    => buildCache(p).flatMap(_ => lookup(p, key))
    }

  private def buildCache(p: Project): IO[Unit] = {
    p.awsExt.lookupFiles
  }

  private def configurationName(p: Project): String = {
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
    go(p.awsExt.lookupBy.split(".").toList)
  }

}

// TODO: Make this class utilise the lookupIndex as described in the aws plugin extension.
// It will need to cache by project after it creates an index for each project it's given on run
// It will need to read in all the files in  the lookupFiles and parse them out normalising the casing to camelCase

