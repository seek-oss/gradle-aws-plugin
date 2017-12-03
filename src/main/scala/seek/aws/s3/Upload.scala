package seek.aws
package s3

import java.io.File

import cats.effect.IO
import fs2.{io, text}
import seek.aws.LazyProperty._

import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class Upload extends AwsTask {

  private val interps = mutable.Map.empty[String, Map[String, Any]]
  def interp(file: String, m: java.util.Map[String, Any]): Unit =
    interps.put(file, m.asScala.toMap)
  def interp(file: File, m: java.util.Map[String, Any]): Unit =
    interps.put(file.getAbsolutePath, m.asScala.toMap)

  private val startToken = lazyProperty[String]("interpTokens", "{{{")
  private val endToken = lazyProperty[String]("interpTokens", "}}}")
  def interpTokens(start: Any, end: Any): Unit = {
    startToken.set(start)
    endToken.set(end)
  }

  protected def maybeInterp(f: File): IO[File] =
    maybeInterp(List(f)).map(_.head)

  protected def maybeInterp(fs: List[File]): IO[List[File]] =
    resolveInterps.flatMap { interps =>
      fs.foldRight(IO.pure(List.empty[File])) { (f, z) =>
        interps.find { case (k, _) => f.getAbsolutePath.matches(k) } match {
          case None         => z.map(f :: _)
          case Some((_, v)) => interp(f, v).flatMap(f => z.map(f :: _))
        }
      }
    }

  private def interp(f: File, tokens: Map[String, String]): IO[File] = {
    val rel = project.getProjectDir.toPath.relativize(f.toPath)
    val out = new File(s"${project.getBuildDir}/interp/${rel}")
    io.file.readAll[IO](f.toPath, 4096)
      .through(text.utf8Decode)
      .through(text.lines)
      .map(searchAndReplace(tokens))
      .intersperse(System.lineSeparator)
      .through(text.utf8Encode)
      .through(io.file.writeAll(out.toPath))
      .run
      .map(_ => out)
  }

  private def searchAndReplace(tokens: Map[String, String])(line: String): String =
    tokens.foldLeft(line) {
      case (z, (k, v)) => z.replace(s"${startToken}${k}${endToken}", v)
    }

  private def resolveInterps: IO[Map[String, Map[String, String]]] =
    interps.foldLeft(IO.pure(Map.empty[String, Map[String, String]])) {
      case (z, (k, v)) =>
        for {
          m  <- z
          vs <- renderValues(v)
        } yield m + (k -> vs)
    }
}
