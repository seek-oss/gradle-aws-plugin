package seek.aws.config
import cats.data.OptionT
import cats.data.OptionT._
import cats.effect.IO
import org.gradle.api.Project

case class LookupMap(map: Map[String, String], key: String) extends Lookup {
  def runOptional(p: Project): OptionT[IO, String] =
    fromOption(map.get(key))
}
