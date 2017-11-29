package seek.aws.autoscaling

import cats.effect.IO
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder
import com.amazonaws.services.autoscaling.model.{AttachLoadBalancerTargetGroupsRequest, AttachLoadBalancersRequest}
import seek.aws.AwsTask

class AttachLoadBalancer extends AwsTask {

  setDescription("Attaches an Auto Scaling group to a Classic or Application Load Balancer")

  private val autoScalingGroup = lazyProp[String]("autoScalingGroup")
  def autoScalingGroup(v: Any): Unit = autoScalingGroup.set(v)

  private val loadBalancer = lazyProp[String]("loadBalancer")
  def loadBalancer(v: Any): Unit = loadBalancer.set(v)

  private val targetGroupArn = lazyProp[String]("targetGroupArn")
  def targetGroupArn(v: Any): Unit = targetGroupArn.set(v)

  private val client = AmazonAutoScalingClientBuilder.standard().withRegion(region).build()

  override def run: IO[Unit] =
    autoScalingGroup.getEither match {
      case Right(autoScalingGroup) =>
        loadBalancer.getEither match {
          case Right(loadBalancer) => attachClassicLoadBalancer(autoScalingGroup, loadBalancer)
          case _ =>
            targetGroupArn.getEither match {
              case Right(targetGroupArn) => attachApplicationLoadBalancer(autoScalingGroup, targetGroupArn)
              case _ => raiseUserCodeError("Either 'loadBalancer' or 'targetGroupArn' must be specified")
            }
        }
      case Left(th) => IO.raiseError(th)
    }

  private def attachClassicLoadBalancer(autoScalingGroup: String, loadBalancer: String): IO[Unit] = {
    logger.lifecycle(s"Attaching '${autoScalingGroup}' to load balancer '${loadBalancer}'")
    val r = new AttachLoadBalancersRequest()
      .withAutoScalingGroupName(autoScalingGroup)
      .withLoadBalancerNames(loadBalancer)
    IO(client.attachLoadBalancers(r))
  }

  private def attachApplicationLoadBalancer(autoScalingGroup: String, targetGroupArn: String): IO[Unit] = {
    logger.lifecycle(s"Attaching '${autoScalingGroup}' to target group '${targetGroupArn}'")
    val r = new AttachLoadBalancerTargetGroupsRequest()
      .withAutoScalingGroupName(autoScalingGroup)
      .withTargetGroupARNs(targetGroupArn)
    IO(client.attachLoadBalancerTargetGroups(r))
  }
}
