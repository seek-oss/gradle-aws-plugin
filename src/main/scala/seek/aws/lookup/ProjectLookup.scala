package seek.aws
package lookup

import cats.effect.IO
import com.typesafe.config.ConfigException.Missing
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

object ProjectLookup {

  private val cache = mutable.Map.empty[Project, Config]

  def lookup(p: Project, key: String, overrides: Map[String, String] = Map.empty): IO[String] =
    if (p.lookupExt.allowProjectOverrides && p.hasProperty(key)) IO.pure(p.property(key).toString)
    else overrides.get(key) match {
      case Some(v) => IO.pure(v)
      case None    => lookupCache(p, key)
    }

  private def lookupCache(p: Project, key: String): IO[String] =
    cache.get(p) match {
      case Some(c) =>
        lookupConfig(c, keyVariations(key))
      case None    =>
        buildCache(p)
        lookupCache(p, key)
    }

  private def lookupConfig(config: Config, keyVariations: List[String]): IO[String] = {
    def go(ks: List[String]): IO[String] =
      ks match {
        case Nil    => raiseError(s"Could not find configuration key ${keyVariations.head}. Tried variations: ${keyVariations.mkString(", ")}")
        case h :: t => IO(config.getString(h)).attempt.flatMap {
          case Right(v)         => IO.pure(v)
          case Left(_: Missing) => go(t)
          case Left(th)         => IO.raiseError(th)
        }
      }
    go(keyVariations)
  }

  private def buildCache(p: Project): Unit =
    cache.put(p, buildConfig(p))

  private def buildConfig(p: Project): Config = {
    @tailrec
    def go(parts: List[String], acc: String = ""): String =
      parts match {
        case h :: t =>
          p.getProperties.asScala.get(h).map(_.asInstanceOf[String]) match {
            case Some(v: String) if v.nonEmpty => go(t, acc + v + ".")
            case _ => throw new GradleException(
              s"Could not find a project property with name ${h}")
          }
        case _ => acc.stripSuffix(".")
      }
    val fname = go(p.lookupExt.key.split('.').toList)
    val matching = p.lookupExt.files.reverse.map(_.getFiles.asScala.filter(_.getName == s"${fname}.conf"))
    val configs = matching.toList.flatMap(_.toList).map(f => ConfigFactory.parseFile(f))
    val c = configs.foldLeft(ConfigFactory.empty())((z, c) => z.withFallback(c))

    println(c)
    c
  }

  private def keyVariations(camelCaseKey: String): List[String] =
    List(camelCaseKey, camelToKebabCase(camelCaseKey), camelToDotCase(camelCaseKey), camelToSnakeCase(camelCaseKey))
}
