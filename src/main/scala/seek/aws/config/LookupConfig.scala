package seek.aws
package config

import java.io.File

import cats.data.OptionT
import cats.data.OptionT._
import cats.effect.IO
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigFactory}
import org.gradle.api.Project
import seek.aws.config.instances._
import seek.aws.config.syntax._

import scala.collection.JavaConverters._
import scala.collection.mutable

case class LookupConfig(key: String) extends Lookup {

  def runOptional(p: Project): OptionT[IO, String] =
    LookupConfig.lookup(p, key)
}

object LookupConfig {

  private val cache = mutable.Map.empty[Project, Config]
  private val validConfigExtensions = "(conf|json|properties)"

  private def lookup(p: Project, key: String): OptionT[IO, String] = {
    val config = cache.get(p) match {
      case Some(c) => IO.pure(c)
      case None    => updateCache(p)
    }
    liftF(config).flatMap(c => lookupConfig(c, keyVariations(key)))
  }

  private def lookupConfig(config: Config, keyVariations: List[String]): OptionT[IO, String] = {
    def go(ks: List[String]): IO[Option[String]] =
      ks match {
        case Nil    => IO.pure(None)
        case h :: t => IO(config.getString(h)).attempt.flatMap {
          case Right(v)         => IO.pure(Some(v))
          case Left(_: Missing) => go(t)
          case Left(th)         => IO.raiseError(th)
        }
      }
    OptionT(go(keyVariations))
  }

  private def keyVariations(camelCaseKey: String): List[String] =
    List(camelCaseKey, camelToKebabCase(camelCaseKey), camelToDotCase(camelCaseKey), camelToSnakeCase(camelCaseKey))

  private def updateCache(p: Project): IO[Config] =
    buildConfig(p).map { c => cache.put(p, c); c }

  private def buildConfig(p: Project): IO[Config] =
    for {
      fs <- sortedConfigFiles(p)
      os <- IO(fs.map(ConfigFactory.parseFile))
      c  <- IO(os.foldLeft(ConfigFactory.empty())(_ withFallback _))
    } yield c

  private def sortedConfigFiles(p: Project): IO[List[File]] = {
    def order(f1: File, f2: File) =
      if (f1.getName.matches(s"${p.cfgExt.commonConfigName}\\.${validConfigExtensions}")) false else true
    def filter(configName: String)(f: File) = {
      val validNames = List(s"${configName}\\.${validConfigExtensions}") ++
        (if (p.cfgExt.allowCommonConfig) List(s"${p.cfgExt.commonConfigName}\\.${validConfigExtensions}") else Nil)
      validNames.exists(regex => f.getName.matches(regex))
    }
    p.cfgExt.configName.map { cn =>
      p.cfgExt.files.flatMap(_.getFiles.asScala.toList.filter(filter(cn)).sortWith(order)).toList.reverse
    }
  }
}
