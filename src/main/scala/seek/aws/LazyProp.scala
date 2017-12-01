package seek.aws

import cats.effect.IO
import groovy.lang.Closure
import org.gradle.api._
import seek.aws.HasLazyProps.lazyProp

class LazyProp[A](name: String, default: Option[A] = None)(project: Project) {

  private var thing: Option[Any] = None

  def run: IO[A] =
    thing match {
      case Some(v) => resolve(v)
      case None    =>
        default match {
          case Some(v) => IO.pure(v)
          case None    => IO.raiseError(new GradleException(s"Property ${name} has not been set"))
        }
    }

  def runOptional: IO[Option[A]] =
    if (isSet) run.map(Some(_))
    else IO.pure(None)

  def set(x: Any) =
    thing = Some(x)

  def isSet: Boolean =
    thing.isDefined

  private def resolve(v: Any): IO[A] =
    v match {
      case l: Lookup     => l.run(project).map(_.asInstanceOf[A])
      case c: Closure[_] => IO(c.call()).flatMap(resolve)
      case a             => IO.pure(a.asInstanceOf[A])
    }
}

object LazyProp {

  def resolve(m: Map[String, Any])(implicit p: Project): IO[Map[String, String]] =
    m.foldLeft(IO.pure(Map.empty[String, String])) {
      case (z, (k, v)) =>
        val lp = lazyProp[String]("")
        lp.set(v)
        for {
          m <- z
          x <- lp.run
        } yield m + (k -> x)
    }
}

trait HasLazyProps {

  def lazyProp[A](name: String)(implicit p: Project): LazyProp[A] =
    new LazyProp[A](name)(p)

  def lazyProp[A](name: String, default: A)(implicit p: Project): LazyProp[A] =
    new LazyProp[A](name, Some(default))(p)
}

object HasLazyProps extends HasLazyProps
