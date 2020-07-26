package seek.aws.config

import cats.data.OptionT._
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import com.amazonaws.services.secretsmanager.model.{GetSecretValueRequest, ResourceNotFoundException}
import com.amazonaws.services.secretsmanager.{AWSSecretsManager, AWSSecretsManagerClientBuilder}
import org.gradle.api.Project
import seek.aws
import seek.aws.instances._

case class LookupSecretsManager(key: String) extends Lookup {

  def runOptional(p: Project): OptionT[IO, String] =
    for {
      c <- liftF(client(p))
      v <- getParameter(key).run(c)
    } yield v

    private def getParameter(key: String) =
      Kleisli[OptionT[IO, ?], AWSSecretsManager, String] { c =>
        OptionT(IO(c.getSecretValue(new GetSecretValueRequest().withSecretId(key))).attempt.map {
          case Right(p)                            => Some(p.getSecretString)
          case Left(_: ResourceNotFoundException) => None
          case Left(th)                            => throw th
        })
      }

  private def client(p: Project): IO[AWSSecretsManager] =
    aws.client.build(AWSSecretsManagerClientBuilder.standard(), p.awsExt.region, p.awsExt.roleArn)
}
