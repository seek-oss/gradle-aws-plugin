package seek.aws.cloudformation

import java.io.File

import cats.effect.IO
import cats.syntax.either._
import io.circe.Json

import scala.io.Source._

object CloudFormationTemplate {

  case class Parameter(name: String, required: Boolean)

  def parseTemplate(f: File): IO[Either[Throwable, Json]] =
    IO(fromFile(f).mkString).map { s =>
      if (f.getName.endsWith(".json"))
        io.circe.parser.parse(s)
      else io.circe.yaml.parser.parse(s)
    }

  def parseParameters(template: File): IO[Either[Throwable, List[Parameter]]] =
    parseTemplate(template).map {
      case Left(th) => Left(th)
      case Right(j) =>
        (j \\ "Parameters").headOption match {
          case None    => Right(Nil)
          case Some(h) =>
            h.asObject match {
              case None    => Left(new Exception("Template has malformed parameters"))
              case Some(o) =>
                o.toMap.foldRight(List.empty[Parameter].asRight[Throwable]) {
                  case ((k, v), z) =>
                    val required = (v \\ "Default").isEmpty
                    z.map(ps => Parameter(k, required) :: ps)
                }
            }
        }
    }
}
