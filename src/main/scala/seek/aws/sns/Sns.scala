package seek.aws.sns

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{
  ListSubscriptionsByTopicResult,
  Subscription
}
import scala.collection.JavaConverters._

sealed trait Sns {

  def subscriptionExists(topicArn: String,
                         protocol: String,
                         endpoint: String): Kleisli[IO, AmazonSNS, Boolean] =
    Kleisli { c =>
      val matcher = subscriptionToTopicExists(topicArn, protocol, endpoint)(_)

      def go(res: IO[ListSubscriptionsByTopicResult]): IO[Boolean] =
        (for {
          r <- res
          res = r.getSubscriptions.asScala.toList match {
            case subs if subs.exists(matcher) => IO(true)
            case _ =>
              Option(r.getNextToken) match {
                case None =>
                  IO(false)
                case t =>
                  go(listSubscriptions(topicArn, t).run(c))
              }
          }
        } yield res).flatMap(identity)

      go(listSubscriptions(topicArn).run(c))
    }

  def subscribe(topicArn: String,
                protocol: String,
                endpoint: String): Kleisli[IO, AmazonSNS, Unit] = Kleisli { c =>
    IO(c.subscribe(topicArn, protocol, endpoint))
  }

  def listSubscriptions(topicArn: String, nextToken: Option[String] = None)
    : Kleisli[IO, AmazonSNS, ListSubscriptionsByTopicResult] =
    Kleisli { c =>
      nextToken match {
        case Some(t) => IO(c.listSubscriptionsByTopic(topicArn, t))
        case None    => IO(c.listSubscriptionsByTopic(topicArn))
      }
    }

  private def subscriptionToTopicExists(
      topicArn: String,
      protocol: String,
      endpoint: String)(s: Subscription): Boolean =
    s.getTopicArn == topicArn && s.getProtocol == protocol && s.getEndpoint == endpoint

}

object Sns extends Sns
