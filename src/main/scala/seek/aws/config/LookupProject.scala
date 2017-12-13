package seek.aws
package config

import java.io.File

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

class LookupProjectFailed(val key: String) extends Exception(s"Could not find value for configuration key ${key}")

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
        case Nil    => IO.raiseError(new LookupProjectFailed(keyVariations.head))
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
    val configObjects = p.cfgExt.files
      .reverse
      .map(_.getFiles.asScala)
      .flatMap(_.toList.filter(configFileFilter(p)).sortWith(configFileSort(p)))
      .map(f => IO(ConfigFactory.parseFile(f)))
      .toList
    configObjects.foldLeft(IO.pure(ConfigFactory.empty())) { (z, c) =>
      for {
        zz <- z
        cc <- c
      } yield zz.withFallback(cc)
    }
  }

  private def configFileFilter(p: Project)(f: File): Boolean = {
    @tailrec
    def buildConfigName(parts: List[String], acc: String = ""): String =
      parts match {
        case h :: t =>
          p.getProperties.asScala.get(h).map(_.asInstanceOf[String]) match {
            case Some(v: String) if v.nonEmpty => buildConfigName(t, acc + v + ".")
            case _ => throw new GradleException(
              s"Project property with name '${h}' is required to build lookup index ${p.cfgExt.index}")
          }
        case _ => acc.stripSuffix(".")
      }
    val cn = buildConfigName(p.cfgExt.index.split('.').toList)
    val validNames = List(s"${cn}\\.${validConfigExtensions}") ++
        (if (p.cfgExt.allowCommonConfig) List(s"${p.cfgExt.commonConfigName}\\.${validConfigExtensions}") else Nil)
    validNames.exists(regex => f.getName.matches(regex))
  }

  private def configFileSort(p: Project)(f1: File, f2: File): Boolean =
    if (f1.getName.matches(s"${p.cfgExt.commonConfigName}\\.${validConfigExtensions}")) false else true
}
