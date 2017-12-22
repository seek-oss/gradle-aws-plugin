package seek.aws.config

import cats.data.OptionT._
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import com.amazonaws.services.simplesystemsmanagement._
import com.amazonaws.services.simplesystemsmanagement.model._
import org.gradle.api.Project
import seek.aws
import seek.aws.instances._
import seek.aws.syntax._

case class LookupParameterStore(key: String) extends Lookup {

  def runOptional(p: Project): OptionT[IO, String] =
    for {
      c <- liftF(client(p))
      v <- getParameter(key).run(c)
    } yield v

    private def getParameter(key: String) =
      Kleisli[OptionT[IO, ?], AWSSimpleSystemsManagement, String] { c =>
        OptionT(IO(c.getParameter(new GetParameterRequest().withName(key).withWithDecryption(true))).attempt.map {
          case Right(p)                            => Some(p.getParameter.getValue)
          case Left(_: ParameterNotFoundException) => None
          case Left(th)                            => throw th
        })
      }

  private def client(p: Project): IO[AWSSimpleSystemsManagement] =
    aws.client.build(AWSSimpleSystemsManagementClientBuilder.standard(), p.awsExt.region, p.awsExt.roleArn)
}
