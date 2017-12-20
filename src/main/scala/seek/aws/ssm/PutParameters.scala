package seek.aws
package ssm

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.simplesystemsmanagement._
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest
import groovy.lang.Closure
import groovy.lang.Closure.DELEGATE_FIRST

import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer

class PutParameters extends AwsTask {

  setDescription("Puts one or more parameters to the Parameter Store")

  private val parameters: ArrayBuffer[IO[Parameter]] = ArrayBuffer.empty
  def parameter(name: Any, closure: Closure[_]): Unit = {
    val bean = new ParameterBean(name)
    closure.setResolveStrategy(DELEGATE_FIRST)
    closure.setDelegate(bean)
    parameters += IO(closure.run()).flatMap(_ => bean.unbeanify)
  }

  override def run: IO[Unit] =
    for {
      r  <- region
      c  <- IO.pure(AWSSimpleSystemsManagementClientBuilder.standard().withRegion(r).build())
      ps <- gather(parameters)
      _  <- putParameters(ps).run(c)
    } yield ()

  private def putParameters(ps: List[Parameter]): Kleisli[IO, AWSSimpleSystemsManagement, Unit] =
    Kleisli { c =>
      ps.foldRight(IO.unit) { (p, z) =>
        z.flatMap { _ =>
          val r = new PutParameterRequest()
            .withName(p.name)
            .withValue(p.value)
            .withDescription(p.description)
            .withType(p.`type`)
            .withKeyId(p.keyId)
            .withOverwrite(p.overwrite)
            .withAllowedPattern(p.allowedPattern)
          IO(c.putParameter(r))
        }
      }
    }

  private class ParameterBean(val name: Any) {
    import LazyProperty.render

    @BeanProperty var value: Any = _
    @BeanProperty var `type`: Any = _
    @BeanProperty var description: Any = _
    @BeanProperty var keyId: Any = _
    @BeanProperty var overwrite: Any = _
    @BeanProperty var allowedPattern: Any = _

    def unbeanify: IO[Parameter] =
      for {
        n <- render[String](name, "name")
        v <- render[String](value, "value")
        t <- render[String](`type`, "type")
        d <- render[String](description, "description", null)
        k <- render[String](keyId, "keyId", null)
        o <- render[Boolean](overwrite, "overwrite", true)
        p <- render[String](allowedPattern, "allowedPattern", null)
      } yield Parameter(n, v, t.toString, d, k, o, p)
  }

  private case class Parameter(
    name: String,
    value: String,
    `type`: String,
    description: String,
    keyId: String,
    overwrite: Boolean,
    allowedPattern: String)
}
