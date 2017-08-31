package seek.aws

import groovy.lang.Closure
import org.gradle.api.InvalidUserCodeException

case class LazyProp(name: String) {
  private var value: Any = null
  private var result: Option[Either[Throwable, String]] = None

  def get: String = getEither.right.get

  def getOption: Option[String] = getEither.toOption

  def getEither: Either[Throwable, String] =
    result match {
      case Some(r) => r
      case None    =>
        result = Some(
          value match {
            case c: Closure[_]  => Right(c.call ().toString)
            case x if x != null => Right(x.toString)
            case _              => Left (new InvalidUserCodeException (s"Task property '${name}' must be set"))
          }
        )
        result.get
    }

  def set(a: Any) = value = a

  def isEmpty: Boolean =
    getOption.isEmpty

  def isDefined: Boolean =
    getOption.isDefined
}
