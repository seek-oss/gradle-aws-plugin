package seek.aws.autoscaling

import cats.effect.IO
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder
import com.amazonaws.services.autoscaling.model.{AttachLoadBalancerTargetGroupsRequest, AttachLoadBalancersRequest}
import seek.aws.{AwsTask, LazyProp}

class AttachLoadBalancer extends AwsTask {

  setDescription("Attaches an Auto Scaling group to a Classic or Application Load Balancer")

  private val autoScalingGroupProp = LazyProp("autoScalingGroup")
  def autoScalingGroup(v: Any) = autoScalingGroupProp.set(v)

  private val loadBalancerProp = LazyProp("loadBalancer")
  def loadBalancer(v: Any): Unit = loadBalancerProp.set(v)

  private val targetGroupArnProp = LazyProp("targetGroupArn")
  def targetGroupArn(v: Any): Unit = targetGroupArnProp.set(v)

  private val client = AmazonAutoScalingClientBuilder.standard().withRegion(region).build()

  override def run(): IO[Unit] =
    if (loadBalancerProp.isDefined)
      attachClassicLoadBalancer(autoScalingGroupProp.get, loadBalancerProp.get)
    else
      attachApplicationLoadBalancer(autoScalingGroupProp.get, targetGroupArnProp.get)

  private def attachClassicLoadBalancer(autoScalingGroup: => String, loadBalancer: => String): IO[Unit] =
    IO {
      logger.lifecycle(s"Attaching '${autoScalingGroup}' to load balancer '${loadBalancer}'")
      val r = new AttachLoadBalancersRequest()
        .withAutoScalingGroupName(autoScalingGroup)
        .withLoadBalancerNames(loadBalancer)
      client.attachLoadBalancers(r)
    }

  private def attachApplicationLoadBalancer(autoScalingGroup: => String, targetGroupArn: => String): IO[Unit] =
    IO {
      logger.lifecycle(s"Attaching '${autoScalingGroup}' to target group '${targetGroupArn}'")
      val r = new AttachLoadBalancerTargetGroupsRequest()
        .withAutoScalingGroupName(autoScalingGroup)
        .withTargetGroupARNs(targetGroupArn)
      client.attachLoadBalancerTargetGroups(r)
    }
}
