package seek.aws.config

import cats.effect.IO
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import org.gradle.api.Project
import seek.aws.instances._
import seek.aws.syntax._

class LookupParameterStore(key: String) extends Lookup {

  def run(p: Project): IO[String] =
    for {
      r <- p.awsExt.region.run
      c <- IO.pure(AWSSimpleSystemsManagementClientBuilder.standard().withRegion(r).build())
      o <- IO(c.getParameter(new GetParameterRequest().withName(key).withWithDecryption(true)))
    } yield o.getParameter.getValue
}
