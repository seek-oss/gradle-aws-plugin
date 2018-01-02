package seek.aws.sns

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.Subscription
import fs2.Stream
import fs2.Stream._

import scala.collection.JavaConverters._

sealed trait Sns {

  def listSubscriptions(topicArn: String) =
    Kleisli[Stream[IO, ?], AmazonSNS, Subscription] { c =>
      case class X(token: Option[String], complete: Boolean)
      val pages = unfoldEval[IO, X, Seq[Subscription]](X(None, false)) {
        case X(_, true)  => IO.pure(None)
        case X(t, false) =>
          IO(c.listSubscriptionsByTopic(topicArn, t.orNull)).map { res =>
            val ss = res.getSubscriptions.asScala.toList
            Option(res.getNextToken) match {
              case t @ Some(_) => Some(ss, X(t, false))
              case _           => Some(ss, X(None, true))
            }
          }
      }
      pages.flatMap(emits(_))
    }
}

object Sns extends Sns
