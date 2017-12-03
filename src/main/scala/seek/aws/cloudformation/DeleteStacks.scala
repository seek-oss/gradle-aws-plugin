package seek.aws
package cloudformation

import cats.data.Kleisli
import cats.data.Kleisli._
import cats.effect.IO
import com.amazonaws.services.cloudformation.model.{DeleteStackRequest, Stack}
import com.amazonaws.services.cloudformation.{AmazonCloudFormation, AmazonCloudFormationClientBuilder}
import fs2.Stream
import seek.aws.cloudformation.instances._
import seek.aws.cloudformation.syntax._

class DeleteStacks extends AwsTask {

  setDescription("Deletes CloudFormation stacks that match a specified regex")

  private val nameMatching = lazyProperty[String]("nameMatching")
  def nameMatching(v: Any): Unit = nameMatching.set(v)

  // TODO: implmenent safety on my default
  private val safetyOn = lazyProperty[Boolean]("safetyOn")
  def safetyOn(v: Any): Unit = safetyOn.set(v)

  override def run: IO[Unit] =
    for {
      r  <- region
      c  <- IO.pure(AmazonCloudFormationClientBuilder.standard().withRegion(r).build())
      n  <- nameMatching.run
      ds <- deleteStacks(n).run(c).runLog
      _  <- waitForStacks(ds.toList).run(c)
    } yield ()

  private def deleteStacks(nameMatching: String): Kleisli[Stream[IO, ?], AmazonCloudFormation, Stack] =
    Kleisli[Stream[IO, ?], AmazonCloudFormation, Stack] { c =>
      describeStacks.run(c).filter(_.getStackName.matches(nameMatching)).map { s =>
        c.deleteStack(new DeleteStackRequest().withStackName(s.getStackName))
        s
      }
    }

  private def waitForStacks(stacks: List[Stack]): Kleisli[IO, AmazonCloudFormation, Unit] =
    stacks match {
      case Nil    => lift(IO.unit)
      case h :: t => waitForStack(h.getStackName).flatMap(_ => waitForStacks(t))
    }
}
