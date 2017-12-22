package seek.aws.config

import cats.effect.IO
import org.gradle.api._

// TODO:
// Ideally we want:
// trait Lookup
// class LookupGradle
// class LookupConfig
// class LookupParameterStore
// class LookupStackOutput
// Then the Lookup object weaves the order together. Each should implement its own caching by
// extending a caching trait
// Can we move the concept of "underrides" up into CreateOrUpdateStack where it belongs?
// Should we have a generic LookupMap?
trait Lookup {
  def run(p: Project): IO[String]
}

class LookupKeyNotFound(val key: String) extends Exception(s"Lookup failed - key '${key}' not found")

object Lookup {
  def lookup(key: String): Lookup =
    lookup(key, Map.empty)

  // TODO:
  private[aws] def lookup(key: String, underrides: Map[String, String]): Lookup = p =>
    LookupProject.lookup(p, key, underrides).attempt.flatMap {
      case Right(v)                   => IO.pure(v)
      case Left(_: LookupKeyNotFound) => parameterStore(key).run(p)
      case Left(th)                   => IO.raiseError(th)
    }

  def stackOutput(stackName: String, key: String): Lookup =
    LookupStackOutput(stackName, key)

  def parameterStore(key: String): Lookup =
    LookupParameterStore(key)
}
