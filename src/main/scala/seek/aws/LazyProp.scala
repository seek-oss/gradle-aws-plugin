package seek.aws

import java.io.File
import java.{lang, util}
import java.util.concurrent.Callable

import cats.effect.IO
import groovy.lang.Closure
import org.codehaus.groovy.runtime.GStringImpl
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.file._
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.{ExtensionContainer, ObjectConfigurationAction}
import org.gradle.api._
import org.gradle.api.internal.plugins.DefaultConvention
import org.gradle.api.reflect.TypeOf
import org.gradle.normalization.InputNormalizationHandler
import org.gradle.process.{ExecSpec, JavaExecSpec}

case class LazyProp[A](name: String, default: Option[A] = None)(project: Project) {

  private var thing: Option[Any] = None

  def run: IO[A] =
    thing match {
      case Some(v) => resolve(v)
      case None    =>
        default match {
          case Some(v) => IO.pure(v)
          case None    => IO.raiseError(new InvalidUserCodeException(s"Property '${name}' has not been set"))
        }
    }

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

trait HasLazyProps {

  def lazyProp[A](name: String)(implicit p: Project): LazyProp[A] =
    LazyProp[A](name)(p)

  def lazyProp[A](name: String, default: A)(implicit p: Project): LazyProp[A] =
    LazyProp[A](name, Some(default))(p)
}

object HasLazyProps extends HasLazyProps
