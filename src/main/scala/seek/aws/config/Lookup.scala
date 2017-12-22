package seek.aws
package config

import cats.data.OptionT
import cats.effect.IO
import org.gradle.api._

trait Lookup { self =>

  def key: String

  def runOptional(p: Project): OptionT[IO, String]

  def run(p: Project): IO[String] =
    runOptional(p).value.flatMap {
      case Some(v) => IO.pure(v)
      case None    => raiseError(s"Could not resolve lookup '${key}'")
    }

  def orElse(that: Lookup): Lookup =
    new Lookup {
      def key: String = self.key
      def runOptional(p: Project): OptionT[IO, String] =
        self.runOptional(p).orElse(that.runOptional(p))
    }
}

object Lookup {
  def lookup(key: String): Lookup =
    LookupGradle(key).orElse(LookupConfig(key)).orElse(LookupParameterStore(key))

  def stackOutput(stackName: String, key: String): Lookup =
    LookupStackOutput(stackName, key)

  def parameterStore(key: String): Lookup =
    LookupParameterStore(key)
}
