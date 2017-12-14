package seek.aws.config

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.simplesystemsmanagement._
import com.amazonaws.services.simplesystemsmanagement.model._
import org.gradle.api.Project
import seek.aws.instances._
import seek.aws.syntax._

case class LookupParameterStore(key: String) extends Lookup {

  def run(p: Project): IO[String] =
    for {
      r <- p.awsExt.region.run
      c <- IO.pure(AWSSimpleSystemsManagementClientBuilder.standard().withRegion(r).build())
      v <- getParameter(key).run(c)
    } yield v

  private def getParameter(key: String): Kleisli[IO, AWSSimpleSystemsManagement, String] =
    Kleisli { c =>
      IO(c.getParameter(new GetParameterRequest().withName(key).withWithDecryption(true))).attempt.map {
        case Left(_: ParameterNotFoundException) => throw new LookupKeyNotFound(key)
        case Left(th)                            => throw th
        case Right(p)                            => p.getParameter.getValue
      }
    }
}
