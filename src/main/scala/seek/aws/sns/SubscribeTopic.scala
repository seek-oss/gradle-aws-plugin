package seek.aws
package sns

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.sns.model.Subscription
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}

import scala.collection.mutable.ArrayBuffer

class SubscribeTopic extends AwsTask {
  import LazyProperty.render
  import Sns._

  setDescription("Adds one or more subscriptions to an SNS topic if they do not exist")

  private val topicArn = lazyProperty[String]("topicArn")
  def topicArn(v: Any): Unit = topicArn.set(v)

  private val pending: ArrayBuffer[IO[PendingSubscription]] = ArrayBuffer.empty
  def subscribe(protocol: Any, endpoint: Any): Unit =
    pending += (for {
      p <- render[String](protocol, "protocol")
      e <- render[String](endpoint, "endpoint")
    } yield PendingSubscription(p, e))

  override def run: IO[Unit] =
    for {
      t  <- topicArn.run
      c  <- buildClient(AmazonSNSClientBuilder.standard())
      ps <- gather(pending)
      es <- listSubscriptions(t).run(c).runLog
      _  <- addSubscriptions(t, uniquePending(ps, es).toList).run(c)
    } yield ()

  private def addSubscriptions(topicArn: String, ps: List[PendingSubscription]): Kleisli[IO, AmazonSNS, Unit] =
    Kleisli { c =>
      ps match {
        case Nil    => IO.unit
        case h :: t =>
          for {
            _ <- IO(c.subscribe(topicArn, h.protocol, h.endpoint))
            _ <- addSubscriptions(topicArn, t).run(c)
          } yield ()
      }
    }

  private def uniquePending(pending: Seq[PendingSubscription], existing: Seq[Subscription]): Set[PendingSubscription] =
    pending.toSet.diff(existing.map(s => PendingSubscription(s.getProtocol, s.getEndpoint)).toSet)

  private case class PendingSubscription(protocol: String, endpoint: String)
}
