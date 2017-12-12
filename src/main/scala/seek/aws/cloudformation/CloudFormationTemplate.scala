package seek.aws.cloudformation

import java.io.File

import cats.effect.IO
import io.circe.Json
import org.gradle.api.GradleException

import scala.io.Source._

object CloudFormationTemplate {

  case class Parameter(name: String, required: Boolean)

  def parseTemplate(f: File): IO[Json] =
    IO(fromFile(f).mkString).map { s =>
      val json =
        if (f.getName.endsWith(".json"))
          io.circe.parser.parse(s)
        else io.circe.yaml.parser.parse(s)
      json match {
        case Left(th) => throw th
        case Right(j) => j
      }
    }

  def parseTemplateParameters(f: File): IO[List[Parameter]] =
    parseTemplate(f).map { j =>
      (j \\ "Parameters").headOption match {
        case None    => Nil
        case Some(h) =>
          h.asObject match {
            case None    => throw new GradleException("Template has malformed parameters")
            case Some(o) =>
              o.toMap.foldRight(List.empty[Parameter]) {
                case ((k, v), z) =>
                  val required = (v \\ "Default").isEmpty
                  Parameter(k, required) :: z
              }
          }
      }
    }
}
