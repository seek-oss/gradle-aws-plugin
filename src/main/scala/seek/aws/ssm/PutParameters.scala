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
    val p = new ParameterBean
    p.name = name
    closure.setResolveStrategy(DELEGATE_FIRST)
    closure.setDelegate(p)
    closure.run()
    println("" + p)
    parameters += p.unbeanify
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

  private class ParameterBean {
    import LazyProperty.render

    @BeanProperty var name: Any = null
    @BeanProperty var value: Any = null
    @BeanProperty var description: Any = null
    @BeanProperty var `type`: Any = null
    @BeanProperty var keyId: Any = null
    @BeanProperty var overwrite: Any = null
    @BeanProperty var allowedPattern: Any = null

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
