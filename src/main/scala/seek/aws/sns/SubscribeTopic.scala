package seek.aws
package sns

import cats.effect.IO
import com.amazonaws.services.sns.AmazonSNSClientBuilder

class SubscribeTopic extends AwsTask {
  import Sns._

  setDescription("Adds a subscription to an SNS topic if it is not present")

  private val topicArn = lazyProperty[String]("topicArn")
  def topicArn(v: Any): Unit = topicArn.set(v)

  private val protocol = lazyProperty[String]("protocol")
  def protocol(v: Any): Unit = protocol.set(v)

  private val endpoint = lazyProperty[String]("endpoint")
  def endpoint(v: Any): Unit = endpoint.set(v)

  override def run: IO[Unit] = {
    for {
      t <- topicArn.run
      p <- protocol.run
      e <- endpoint.run
      c <- buildClient(AmazonSNSClientBuilder.standard())
      exists <- subscriptionExists(t, p, e).run(c)
      res <- exists match {
        case false =>
          logger.lifecycle(
            s"no existing subscription found for $e, creating a new one to topic $t")
          subscribe(t, p, e).run(c)
        case true =>
          logger.lifecycle(s"existing subscription found for $e, skipping")
          IO.unit
      }
    } yield res
  }
}
