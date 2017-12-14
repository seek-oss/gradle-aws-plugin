package seek.aws
package ssm

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.simplesystemsmanagement._
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest
import groovy.lang.Closure

import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer

class PutParameters extends AwsTask {

  setDescription("Puts one or more parameters to the Parameter Store")

  private val parameters: ArrayBuffer[IO[Parameter]] = ArrayBuffer.empty
  def parameter(name: Any, closure: Closure[_]): Unit = {
    val p = new ParameterBean(name)
    closure.setDelegate(p)
    closure.run()
    parameters += p.render
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

  private def gather[A](ios: Seq[IO[A]]): IO[List[A]] =
    ios.foldRight(IO.pure(List.empty[A]))((p, z) => z.flatMap(zz => p.map(_ :: zz)))

  private class ParameterBean(
    @BeanProperty var name: Any,
    @BeanProperty var value: Any,
    @BeanProperty var description: Any,
    @BeanProperty var `type`: Any,
    @BeanProperty var keyId: Any,
    @BeanProperty var overwrite: Any,
    @BeanProperty var allowedPattern: Any) {
    def this(name: Any) = this(name, null, null, null, null, true, null)
    import LazyProperty.{render => render1}
    def render: IO[Parameter] =
      for {
        n <- render1[String](name)
        v <- render1[String](value)
        d <- render1[String](description)
        t <- render1[String](`type`)
        k <- render1[String](keyId)
        o <- render1[Boolean](overwrite)
        p <- render1[String](allowedPattern)
      } yield Parameter(n, v, d, t, k, o, p)
  }

  private case class Parameter(
    name: String,
    value: String,
    description: String,
    `type`: String,
    keyId: String,
    overwrite: Boolean,
    allowedPattern: String)
}
