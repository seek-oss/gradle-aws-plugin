package seek.aws

import java.lang.Thread.sleep

import cats.data.Kleisli
import cats.data.Kleisli._
import cats.effect.IO
import com.amazonaws.services.cloudformation.AmazonCloudFormation
import com.amazonaws.services.cloudformation.model.{ListStacksRequest, StackSummary}
import fs2.Stream
import org.gradle.api.Project

import scala.collection.JavaConverters._

package object cloudformation {

  object syntax extends HasCloudFormationPluginExtension.ToHasCloudFormationPluginExtensionOps

  object instances {
    implicit val projectHasCloudFormationPluginExtension = new HasCloudFormationPluginExtension[Project] {
      def cfnExt(p: Project) =
        p.getExtensions.getByType(classOf[CloudFormationPluginExtension])
    }
  }

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
    listStacks.mapF(_.filter(_.getStackName == stackName).runLast.map(_.map(s => StackStatus(s.getStackStatus))))

  def waitForStack(stackName: String): Kleisli[IO, AmazonCloudFormation, Unit] =
    stackStatus(stackName).flatMap {
      case None                           => lift(raiseError(s"Stack ${stackName} does not exist"))
      case Some(s: FailedStackStatus)     => lift(raiseError(s"Stack ${stackName} failed with status ${s.name}"))
      case Some(_: CompleteStackStatus)   => lift(IO.unit)
      case Some(_: InProgressStackStatus) =>
        sleep(1000)
        waitForStack(stackName)
    }
}
