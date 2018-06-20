package seek.aws.sns

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import seek.aws.{AwsTask, LazyProperty, gather}

import scala.collection.mutable.ArrayBuffer

class SetTopicAttributes extends AwsTask {
  import LazyProperty.render

  setDescription("Adds one or more attributes to an SNS topic and updates if they already exist")

  private val topicArn = lazyProperty[String]("topicArn")
  def topicArn(v: Any): Unit = topicArn.set(v)

  private val pending: ArrayBuffer[IO[PendingAttribute]] = ArrayBuffer.empty
  def addAttribute(name: Any, value: Any): Unit =
    pending += (for {
      n <- render[String](name, "name")
      v <- render[String](value, "value")
    } yield PendingAttribute(n, v))

  protected def run: IO[Unit] = for {
    arn <- topicArn.run
    c <- buildClient(AmazonSNSClientBuilder.standard())
    pas <- gather(pending)
    _ <- addAttributes(arn, pas).run(c)
  } yield ()

  private def addAttributes(arn: String, pas: List[PendingAttribute]): Kleisli[IO, AmazonSNS, Unit] =
    pas match {
      case Nil => Kleisli.liftF(IO.unit)
      case h :: t =>
        for {
          _ <- setAttribute(arn, h)
          _ <- addAttributes(arn, t)
        } yield ()
    }

  private def setAttribute(arn: String, pa: PendingAttribute): Kleisli[IO, AmazonSNS, Unit] =
    Kleisli { c =>
      IO(c.setTopicAttributes(arn, pa.name, pa.value))
    }

  private case class PendingAttribute(name: String, value: String)
}
