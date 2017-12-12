package seek.aws
package config

import cats.effect.IO
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigFactory}
import org.gradle.api.{GradleException, Project}
import seek.aws.config.instances._
import seek.aws.config.syntax._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable

class LookupProject(key: String) extends Lookup {

  def run(p: Project): IO[String] =
    LookupProject.lookup(p, key)
}

object LookupProject {

  private val cache = mutable.Map.empty[Project, Config]

  def lookup(p: Project, key: String, underrides: Map[String, String] = Map.empty): IO[String] =
    if (p.configExt.allowProjectOverrides && p.hasProperty(key)) IO.pure(p.property(key).toString)
    else underrides.get(key) match {
      case Some(v) => IO.pure(v)
      case None    => lookupCache(p, key)
    }

  private def lookupCache(p: Project, key: String): IO[String] = {
    val config = cache.get(p) match {
      case Some(c) => IO.pure(c)
      case None    => updateCache(p)
    }
    config.flatMap(c => lookupConfig(c, keyVariations(key)))
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

  private def updateCache(p: Project): IO[Config] =
    buildConfig(p).map { c => cache.put(p, c); c }

  private def buildConfig(p: Project): IO[Config] = {
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
    val filename = go(p.configExt.lookupBy.split('.').toList) + ".conf"
    val configFiles = p.configExt.files.reverse.map(_.getFiles.asScala).flatMap(_.toList).toList
    val configObjects = configFiles.filter(_.getName == filename).map(f => IO(ConfigFactory.parseFile(f)))
    configObjects.foldLeft(IO.pure(ConfigFactory.empty()))((z, c) => for { zz <- z; cc <- c } yield zz.withFallback(cc))
  }

  private def keyVariations(camelCaseKey: String): List[String] =
    List(camelCaseKey, camelToKebabCase(camelCaseKey), camelToDotCase(camelCaseKey), camelToSnakeCase(camelCaseKey))
}
