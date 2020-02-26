package seek.aws

import cats.data.OptionT
import cats.data.OptionT._
import cats.effect.IO
import groovy.lang.{Closure, GString}
import org.gradle.api._
import seek.aws.HasLazyProperties.lazyProperty
import seek.aws.config.Lookup

import scala.reflect.ClassTag

class LazyProperty[A](name: String, default: Option[A] = None)(project: Project) {

  private var thing: Option[Any] = None

  def run(implicit tag: ClassTag[A]): IO[A] =
    thing match {
      case Some(v) => render(v)
      case None =>
        default match {
          case Some(v) => IO.pure(v)
          case None    => IO.raiseError(new GradleException(s"Property ${name} has not been set"))
        }
    }

  def runOptional(implicit tag: ClassTag[A]): OptionT[IO, A] =
    if (isSet) liftF(run) else none

  def set(x: Any): Unit =
    thing = Option(x)

  def isSet: Boolean =
    thing.isDefined

  def orElse(that: LazyProperty[A]): LazyProperty[A] =
    if (isSet) this else that

  private def render(v: Any)(implicit tag: ClassTag[A]): IO[A] =
    v match {
      case l: Lookup     => l.run(project).flatMap(render)
      case c: Closure[_] => IO(c.call()).flatMap(render)
      case g: GString    => render(g.toString)
      case a: A          => IO.pure(a)
      case null          => raiseError(s"Unexpected null value for property ${name}")
    }
}

object LazyProperty {

  def render[A](a: Any, name: String)(implicit p: Project, tag: ClassTag[A]): IO[A] = {
    val lp = lazyProperty[A](name)
    lp.set(a)
    lp.run
  }

  def renderOptional[A](a: Any, name: String)(implicit p: Project, tag: ClassTag[A]): IO[Option[A]] = {
    val lp = lazyProperty[A](name)
    lp.set(a)
    lp.runOptional.value
  }

  def render[A](a: Any, name: String, default: A)(implicit p: Project, tag: ClassTag[A]): IO[A] =
    renderOptional(a, name).map(_.getOrElse(default))

  def renderAll[A](s: List[Any])(implicit p: Project, tag: ClassTag[A]): IO[List[A]] =
    s.foldRight(IO.pure(List.empty[A])) { (a, z) =>
      for {
        h <- render[A](a, "")
        t <- z
      } yield h :: t
    }

  def renderValues[K, A](m: Map[K, Any])(implicit p: Project, tag: ClassTag[A]): IO[Map[K, A]] =
    renderAll[A](m.values.toList).map(rs => m.keys.zip(rs).toMap)

  def renderValuesOptional[K, A](m: Map[K, Any])(implicit p: Project, tag: ClassTag[A]): IO[Option[Map[K, A]]] =
    if (m.nonEmpty) renderValues(m).map(vs => Some(vs)) else IO.pure(None)
}

trait HasLazyProperties {

  def lazyProperty[A](name: String)(implicit p: Project): LazyProperty[A] =
    new LazyProperty[A](name)(p)

  def lazyProperty[A](name: String, default: A)(implicit p: Project): LazyProperty[A] =
    new LazyProperty[A](name, Some(default))(p)
}

object HasLazyProperties extends HasLazyProperties
