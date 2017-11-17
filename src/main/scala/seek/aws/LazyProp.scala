package seek.aws

import groovy.lang.Closure
import org.gradle.api.InvalidUserCodeException

private[aws] case class LazyProp[A](name: String, lookupPrefix: String, props: ProjectProps, default: Option[A] = None) {

  private var run: Option[Any] = None
  private var cache: Option[Either[Throwable, A]] = None

  def get: A = getEither.right.get

  def getOption: Option[A] = getEither.toOption

  def getEither: Either[Throwable, A] =
    cache match {
      case Some(r) => r
      case None    =>
        cache = Some(
          run match {
            case Some(v) => resolve(v)
            case None    =>
              default match {
                case Some(v) => Right(v)
                case None    => Left(new InvalidUserCodeException(s"Task property '${name}' must be set"))
              }
          })
        cache.get
    }

  def set(x: Any) =
    run = Some(x)

  def isEmpty: Boolean =
    getOption.isEmpty

  def isDefined: Boolean =
    getOption.isDefined

  private def resolve(run: Any): Either[Throwable, A] =
    try run match {
      case c: Closure[_]     => resolve(c.call())
      case p: LookupProperty => resolve(p.get(props, lookupPrefix))
      case v                 => Right(v.asInstanceOf[A])
    } catch {
      case th: Throwable => Left(th)
    }
}
