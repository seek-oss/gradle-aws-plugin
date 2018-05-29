package seek.aws
package sns

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.sns.model.{SetSubscriptionAttributesRequest, Subscription}
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

class SubscribeTopic extends AwsTask {
  import LazyProperty.{render, renderOptional}
  import Sns._

  setDescription("Adds one or more subscriptions to an SNS topic if they do not exist")

  private val topicArns = lazyProperty[String]("topicArns")
  def topicArn(v: Any): Unit = topicArns.set(v)
  def topicArns(v: Any): Unit = topicArns.set(v)

  private val pending: ArrayBuffer[IO[PendingSubscription]] = ArrayBuffer.empty
  def subscribe(protocol: Any, endpoint: Any, filterPolicy: Any): Unit =
    pending += (for {
      p  <- render[String](protocol, "protocol")
      e  <- render[String](endpoint, "endpoint")
      fp <- renderOptional[String](filterPolicy, "filterPolicy")
    } yield PendingSubscription(p, e, fp, None))
  def subscribe(protocol: Any, endpoint: Any): Unit =
    subscribe(protocol, endpoint, null)

  override def run: IO[Unit] =
    for {
      arns <- topicArns.run
      ts = split(arns)
      c  <- buildClient(AmazonSNSClientBuilder.standard())
      ps <- gather(pending)
      _  <- subscribeToTopics(ts, ps).run(c)
    } yield ()


  private def subscribeToTopics(ts: List[String], ps: List[PendingSubscription]): Kleisli[IO, AmazonSNS, Unit] =
    Kleisli { c =>
      ts.foldLeft(IO.unit) { (z, t) =>
        for {
          _  <- z
          es <- listSubscriptions(t).run(c).compile.toList
          _  <- updateSubscriptions(t, merge(ps, es)).run(c)
        } yield ()
    }
  }

  private def updateSubscriptions(topicArn: String, ps: List[PendingSubscription]): Kleisli[IO, AmazonSNS, Unit] =
    ps match {
      case Nil    => Kleisli.liftF(IO.unit)
      case h :: t => for {
        arn <- maybeSubscribeToTopic(topicArn, h)
        _   <- maybeUpdateFilterPolicy(arn, h.filterPolicy)
        _   <- updateSubscriptions(topicArn, t)
      } yield ()
    }

  private def maybeSubscribeToTopic(topicArn: String, ps: PendingSubscription): Kleisli[IO, AmazonSNS, String] =
    Kleisli { c =>
      ps.arn match {
        case Some(a) => IO.pure(a)
        case None => IO(c.subscribe(topicArn, ps.protocol, ps.endpoint).getSubscriptionArn)
      }
    }

  private def maybeUpdateFilterPolicy(arn: String, policy: Option[String]): Kleisli[IO, AmazonSNS, Unit] =
    Kleisli { c =>
      policy match {
        case Some(p) => IO {
          val r = new SetSubscriptionAttributesRequest(arn, "FilterPolicy", p)
          c.setSubscriptionAttributes(r)
        }
        case None => IO.unit
      }
    }

  private def merge(pending: List[PendingSubscription], existing: List[Subscription]): List[PendingSubscription] =
    existing.foldLeft(pending) { (z, es) =>
      z.zipWithIndex
        .find(psi => {
          val (ps, _) = psi
          ps.protocol == es.getProtocol && ps.endpoint == es.getEndpoint
        })
        .map(psi => {
          val (ps, i) = psi
          z.updated(i, ps.copy(arn = Some(es.getSubscriptionArn)))
        })
        .getOrElse(z)
    }

  private def split(s: String, sep: String = ","): List[String] =
    s.split(sep).toList.map(_.trim).filterNot(_.isEmpty)

  private trait SnsSubscription {
    val protocol: String
    val endpoint: String
    val filterPolicy: Option[String]
  }

  private case class PendingSubscription(protocol: String, endpoint: String, filterPolicy: Option[String], arn: Option[String])
}
