package seek.aws
package s3

import java.io.File
import java.util.regex.Pattern.quote

import cats.effect.IO
import fs2.{io, text}
import groovy.lang.Closure
import groovy.lang.Closure.DELEGATE_FIRST
import org.gradle.api.file.FileCollection
import seek.aws.LazyProperty._
import seek.aws.config.LookupProject

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

abstract class Upload extends AwsTask {

  private val interpolations: ArrayBuffer[IO[Interpolation]] = ArrayBuffer.empty

  def interpolate(fs: FileCollection, closure: Closure[_]): Unit =
    addInterpolation(fileTreeElements(fs).map(_.getFile), closure)

  def interpolate(f: File, closure: Closure[_]): Unit =
    addInterpolation(List(f), closure)

  private def addInterpolation(fs: List[File], closure: Closure[_]): Unit = {
    val bean = new InterpolationBean(fs)
    closure.setResolveStrategy(DELEGATE_FIRST)
    closure.setDelegate(bean)
    interpolations += IO(closure.run()).flatMap(_ => bean.unbeanify)
  }

  protected def maybeInterpolate(f: File): IO[File] =
    maybeInterpolate(List(f)).map(_.head)

  protected def maybeInterpolate(fs: List[File]): IO[List[File]] =
    gather(interpolations).flatMap { is =>
      fs.foldRight(IO.pure(List.empty[File])) { (f, z) =>
        is.find(_.files.contains(f)) match {
          case None    => z.map(f :: _)
          case Some(i) => for { zz <- z; ff <- runInterpolation(f, i) } yield ff :: zz
        }
      }
    }

  private def runInterpolation(f: File, i: Interpolation): IO[File] = {
    val rel = project.getProjectDir.toPath.relativize(f.toPath)
    val out = new File(s"${project.getBuildDir}/interpolated/${rel}")
    io.file.readAll[IO](f.toPath, 4096)
      .through(text.utf8Decode)
      .through(text.lines)
      .evalMap(searchAndReplace(i))
      .intersperse(System.lineSeparator)
      .through(text.utf8Encode)
      .through(io.file.writeAll(out.toPath))
      .run
      .map(_ => out)
  }

  private def searchAndReplace(i: Interpolation)(line: String): IO[String] = {
    i.pattern.findAllMatchIn(line).foldLeft(IO.pure(line)) { (z, t) =>
      val token = t.matched.stripPrefix(i.startToken).stripSuffix(i.endToken)
      val replacement = i.replace.get(token) match {
        case Some(v) => IO.pure(v)
        case None    => LookupProject.lookup(project, token)
      }
      for {
        r <- replacement
        zz <- z
      } yield zz.replace(t.matched, r)
    }
  }

  private class InterpolationBean(val files: List[File]) {
    import LazyProperty.render

    @BeanProperty var startToken: Any = "{{{"
    @BeanProperty var endToken: Any = "}}}"
    @BeanProperty var replace: java.util.Map[String, Any] = Map.empty[String, Any].asJava

    def unbeanify: IO[Interpolation] =
      for {
        st <- render[String](startToken, "startToken")
        et <- render[String](endToken, "endToken")
        m  <- renderValues[String, String](replace.asScala.toMap)
      } yield Interpolation(files, st, et, m)
  }

  private case class Interpolation(files: List[File], startToken: String, endToken: String, replace: Map[String, String]) {
    def pattern: Regex =
      s"${quote(startToken)}[a-zA-Z0-9\\-_\\.\\/]+${quote(endToken)}".r
  }
}
