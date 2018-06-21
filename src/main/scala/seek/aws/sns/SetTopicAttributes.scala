package seek.aws.sns

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import seek.aws.{AwsTask, LazyProperty, gather}
import scala.collection.mutable.ArrayBuffer

class SetTopicAttributes extends AwsTask {
  import LazyProperty.render

  setDescription("Sets one or more attributes on an SNS topic and updates if they already exist")

  private val topicArn = lazyProperty[String]("topicArn")
  def topicArn(v: Any): Unit = topicArn.set(v)

  private val attributes: ArrayBuffer[IO[Attribute]] = ArrayBuffer.empty
  def attribute(name: Any, value: Any): Unit =
    attributes += (for {
      n <- render[String](name, "name")
      v <- render[String](value, "value")
    } yield Attribute(n, v))

  protected def run: IO[Unit] = for {
    arn <- topicArn.run
    c   <- buildClient(AmazonSNSClientBuilder.standard())
    as  <- gather(attributes)
    _   <- setAttributes(arn, as).run(c)
  } yield ()

  private def setAttributes(arn: String, as: List[Attribute]): Kleisli[IO, AmazonSNS, Unit] =
    as match {
      case Nil => Kleisli.liftF(IO.unit)
      case h :: t =>
        for {
          _ <- setAttribute(arn, h)
          _ <- setAttributes(arn, t)
        } yield ()
    }

  private def setAttribute(arn: String, pa: Attribute): Kleisli[IO, AmazonSNS, Unit] =
    Kleisli { c =>
      IO(c.setTopicAttributes(arn, pa.name, pa.value))
    }

  private case class Attribute(name: String, value: String)
}
