package seek.aws
package autoscaling

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.services.autoscaling.model.{AttachLoadBalancerTargetGroupsRequest, AttachLoadBalancersRequest}
import com.amazonaws.services.autoscaling.{AmazonAutoScaling, AmazonAutoScalingClientBuilder}

class AttachLoadBalancer extends AwsTask {

  setDescription("Attaches an Auto Scaling Group to a Classic or Application Load Balancer")

  private val autoScalingGroup = lazyProperty[String]("autoScalingGroup")
  def autoScalingGroup(v: Any): Unit = autoScalingGroup.set(v)

  private val loadBalancer = lazyProperty[String]("loadBalancer")
  def loadBalancer(v: Any): Unit = loadBalancer.set(v)

  private val targetGroupArn = lazyProperty[String]("targetGroupArn")
  def targetGroupArn(v: Any): Unit = targetGroupArn.set(v)

  override def run: IO[Unit] =
    for {
      r   <- region
      asg <- autoScalingGroup.run
      c   <- IO.pure(AmazonAutoScalingClientBuilder.standard().withRegion(r).build())
      _   <- (loadBalancer.isSet, targetGroupArn.isSet) match {
        case (true, false) => loadBalancer.run.map(lb => attachV1(asg, lb).run(c))
        case (false, true) => targetGroupArn.run.map(tg => attachV2(asg, tg).run(c))
        case _             => raiseError("Either loadBalancer or targetGroupArn must be specified but not both")

      }
    } yield ()

  private def attachV1(autoScalingGroup: String, loadBalancer: String): Kleisli[IO, AmazonAutoScaling, Unit] =
    Kleisli { c =>
      val r = new AttachLoadBalancersRequest()
        .withAutoScalingGroupName(autoScalingGroup)
        .withLoadBalancerNames(loadBalancer)
      IO(c.attachLoadBalancers(r))
    }

  private def attachV2(autoScalingGroup: String, targetGroupArn: String): Kleisli[IO, AmazonAutoScaling, Unit] =
    Kleisli { c =>
      val r = new AttachLoadBalancerTargetGroupsRequest()
        .withAutoScalingGroupName(autoScalingGroup)
        .withTargetGroupARNs(targetGroupArn)
      IO(c.attachLoadBalancerTargetGroups(r))
    }
}
