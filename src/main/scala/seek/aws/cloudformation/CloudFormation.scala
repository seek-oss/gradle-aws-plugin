package seek.aws
package cloudformation

import java.lang.Thread.sleep
import java.time.Instant.now

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.cloudformation.AmazonCloudFormation
import com.amazonaws.services.cloudformation.model.{DescribeStacksRequest, Stack}
import fs2.Stream
import fs2.Stream.{emits, unfoldEval}
import org.gradle.api.GradleException

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, _}

sealed trait CloudFormation {

  def stackStatus(stackName: String): Kleisli[IO, AmazonCloudFormation, Option[StackStatus]] =
    Kleisli { c =>
      IO(c.describeStacks(new DescribeStacksRequest().withStackName(stackName))).attempt.flatMap {
        case Right(s) =>
          IO.pure(s.getStacks.asScala.headOption.map(s => StackStatus(s.getStackStatus)))
        case Left(e)  =>
          if (e.getMessage.contains("does not exist")) IO.pure(None)
          else IO.raiseError(e)
      }
    }

  def describeStacks: Kleisli[Stream[IO, ?], AmazonCloudFormation, Stack] =
    Kleisli[Stream[IO, ?], AmazonCloudFormation, Stack] { c =>
      case class X(token: Option[String], complete: Boolean)
      val pages = unfoldEval[IO, X, Seq[Stack]](X(None, false)) {
        case X(_, true)  => IO.pure(None)
        case X(t, false) =>
          val req = new DescribeStacksRequest().withNextToken(t.orNull)
          IO(c.describeStacks(req)).map { res =>
            val ss = res.getStacks.asScala
            Option(res.getNextToken) match {
              case t @ Some(_) => Some(ss, X(t, false))
              case _           => Some(ss, X(t, true))
            }
          }
      }
      pages.flatMap(emits(_))
    }

  def waitForStack(stackName: String, timeout: Duration, checkEvery: Duration = 3.seconds):
      Kleisli[IO, AmazonCloudFormation, Unit] = Kleisli { c =>
    for {
      _  <- if (timeout < 0.seconds) raiseError(s"Timed out waiting for stack ${stackName}") else IO.unit
      t1 <- IO(now.getEpochSecond.seconds)
      _  <- IO(sleep(checkEvery.toMillis))
      ss <- stackStatus(stackName).run(c)
      t2 <- IO(now.getEpochSecond.seconds)
      to <- IO(timeout - (t2 - t1))
      _  <- ss match {
        case None | Some(CreateComplete | UpdateComplete | DeleteComplete) => IO.unit
        case Some(_: InProgressStackStatus) => waitForStack(stackName, to).run(c)
        case Some(s) => raiseError(s"Stack ${stackName} failed with status ${s.name}")
      }
    } yield ()
  }
}

object CloudFormation extends CloudFormation
