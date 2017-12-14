package seek.aws.config

import cats.effect.IO
import org.gradle.api._

trait Lookup {
  def run(p: Project): IO[String]
}

class LookupKeyNotFound(val key: String) extends Exception(s"Lookup failed - key ${key} not found")

object Lookup {
  def lookup(key: String): Lookup = p =>
    LookupProject(key).run(p).attempt.flatMap {
      case Left(_: LookupKeyNotFound) => parameterStore(key).run(p)
      case Left(th)                   => IO.raiseError(th)
      case Right(v)                   => IO.pure(v)
    }

  def stackOutput(stackName: String, key: String): Lookup =
    LookupStackOutput(stackName, key)

  def parameterStore(key: String): Lookup =
    LookupParameterStore(key)
}
