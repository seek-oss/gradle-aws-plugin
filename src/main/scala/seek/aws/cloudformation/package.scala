package seek.aws

import java.lang.Thread.sleep

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.cloudformation.AmazonCloudFormation
import com.amazonaws.services.cloudformation.model.{DescribeStacksRequest, ListStacksRequest, StackSummary}
import fs2.Stream
import org.gradle.api.Project

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, _}

package object cloudformation {

  object syntax extends HasCloudFormationPluginExtension.ToHasCloudFormationPluginExtensionOps

  object instances {
    implicit val projectHasCloudFormationPluginExtension = new HasCloudFormationPluginExtension[Project] {
      def cfnExt(p: Project) =
        p.getExtensions.getByType(classOf[CloudFormationPluginExtension])
    }
  }

  // Is this useful? Takes a really long time in some accounts
  def listStacks: Kleisli[Stream[IO, ?], AmazonCloudFormation, StackSummary] =
    Kleisli[Stream[IO, ?], AmazonCloudFormation, StackSummary] { c =>
      case class X(token: Option[String], complete: Boolean)
      val pages = Stream.unfoldEval[IO, X, Seq[StackSummary]](X(None, false)) {
        case X(_, true)  => IO.pure(None)
        case X(t, false) =>
          val req = new ListStacksRequest().withNextToken(t.orNull)
          IO(c.listStacks(req)).map { res =>
            val ss = res.getStackSummaries.asScala
            Option(res.getNextToken) match {
              case t @ Some(_) => Some(ss, X(t, false))
              case _           => Some(ss, X(t, true))
            }
        }
      }
      pages.flatMap(Stream.emits(_))
    }

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

  def waitForStack(stackName: String, checkEvery: Duration = 3.seconds): Kleisli[IO, AmazonCloudFormation, Unit] =
    Kleisli { c =>
      for {
        _  <- IO(sleep(checkEvery.toMillis))
        ss <- stackStatus(stackName).run(c)
        _  <- ss match {
          case None                         => IO.unit // Assume stack is in the delete-complete state
          case Some(_: CompleteStackStatus) => IO.unit
          case Some(s: FailedStackStatus)   => raiseError(s"Stack ${stackName} failed with status ${s.name}")
          case _                            => waitForStack(stackName).run(c)
        }
      } yield ()
    }
}
