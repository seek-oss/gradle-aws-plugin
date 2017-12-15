package seek.aws
package config

import java.io.File

import cats.effect.IO
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigFactory}
import org.gradle.api.Project
import seek.aws.config.instances._
import seek.aws.config.syntax._

import scala.collection.JavaConverters._
import scala.collection.mutable

case class LookupProject(key: String) extends Lookup {

  def run(p: Project): IO[String] =
    LookupProject.lookup(p, key)
}

object LookupProject {

  private val cache = mutable.Map.empty[Project, Config]
  private val validConfigExtensions = "(conf|json|properties)"
  private val reservedGradleProperties = List("project")

  def lookup(p: Project, key: String, underrides: Map[String, String] = Map.empty): IO[String] =
    if (p.cfgExt.allowProjectOverrides && !reservedGradleProperties.contains(key) && p.hasProperty(key))
      IO.pure(p.property(key).toString)
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
        case Nil    => IO.raiseError(new LookupKeyNotFound(keyVariations.head))
        case h :: t => IO(config.getString(h)).attempt.flatMap {
          case Right(v)         => IO.pure(v)
          case Left(_: Missing) => go(t)
          case Left(th)         => IO.raiseError(th)
        }
      }
    go(keyVariations)
  }

  private def keyVariations(camelCaseKey: String): List[String] =
    List(camelCaseKey, camelToKebabCase(camelCaseKey), camelToDotCase(camelCaseKey), camelToSnakeCase(camelCaseKey))

  private def updateCache(p: Project): IO[Config] =
    buildConfig(p).map { c => cache.put(p, c); c }

  private def buildConfig(p: Project): IO[Config] = {
    val configFiles = gather(
      p.cfgExt.files.reverse
        .map(_.getFiles.asScala)
        .flatMap(_.toList.map(IO.pure)))
    for {
      cn <- p.cfgExt.configName
      fs <- configFiles.map(_.filter(configFileFilter(p, cn)).sortWith(configFileSort(p)))
      os <- IO(fs.map(ConfigFactory.parseFile))
      c  <- IO(os.foldLeft(ConfigFactory.empty())(_ withFallback _))
    } yield c
  }

  private def configFileFilter(p: Project, configName: String)(f: File): Boolean = {
    val validNames = List(s"${configName}\\.${validConfigExtensions}") ++
        (if (p.cfgExt.allowCommonConfig) List(s"${p.cfgExt.commonConfigName}\\.${validConfigExtensions}") else Nil)
    validNames.exists(regex => f.getName.matches(regex))
  }

  private def configFileSort(p: Project)(f1: File, f2: File): Boolean =
    if (f1.getName.matches(s"${p.cfgExt.commonConfigName}\\.${validConfigExtensions}")) false else true
}
