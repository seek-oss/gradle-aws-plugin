package seek.aws

import cats.effect.IO
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import com.amazonaws.services.cloudformation.{AmazonCloudFormation, AmazonCloudFormationClientBuilder}
import org.gradle.api._
import seek.aws.instances._
import seek.aws.syntax._

import scala.annotation.tailrec
import scala.collection.JavaConverters._

sealed trait Lookup {
  def run(p: Project): IO[String]
}

object Lookup {
  def lookup(key: String): Lookup =
    new PropertyLookup(key)

  def stackOutput(stackName: String, key: String): Lookup =
    new CloudFormationStackOutputLookup(stackName, key)
}

class PropertyLookup(key: String) extends Lookup {

  class MissingPropertyException(p: String) extends GradleException(s"Property ${p} expected but not defined")

  def run(p: Project): IO[String] =
    IO(property(p, key)).map {
      case Some(v) => v
      case None    =>
        val fullKey = buildKey(p, lookupPrefix(p).split('.').toList)
        property(p, fullKey).getOrElse(throw new MissingPropertyException(fullKey))
    }

  @tailrec
  private def buildKey(p: Project, prefixParts: List[String], acc: String = ""): String =
    prefixParts match {
      case h :: t =>
        property(p, h) match {
          case Some(v: String) if v.nonEmpty => buildKey(p, t, acc + v + ".")
          case _ => throw new MissingPropertyException(h)
        }
      case _ => acc + key
    }

  private def property(p: Project, key: String): Option[String] =
    p.getProperties.asScala.get(key) match {
      case Some(v) => Some(v.asInstanceOf[String])
      case _       => None
    }

  private def lookupPrefix(p: Project): String =
    p.getExtensions.getByType(classOf[AwsPluginExtension]).lookupPrefix

}

class CloudFormationStackOutputLookup(stackName: String, key: String) extends Lookup {

  def run(p: Project): IO[String] =
    for {
      r <- p.awsExt.region.run
      c <- IO.pure(client(r))
      o <- stackOutput(c, r)
    } yield o

  private def client(region: String): AmazonCloudFormation =
    AmazonCloudFormationClientBuilder.standard().withRegion(region).build()

  private def stackOutput(c: AmazonCloudFormation, region: String): IO[String] = {
    IO(c.describeStacks(new DescribeStacksRequest().withStackName(stackName))).map { r =>
      r.getStacks.asScala.headOption match {
        case None    => throw new GradleException(s"Stack ${stackName} does not exist in region ${region}")
        case Some(h) => h.getOutputs.asScala.find(_.getOutputKey == key) match {
          case None    => throw new GradleException(s"Stack ${stackName} does not have output key ${key}")
          case Some(o) => o.getOutputValue
        }
      }
    }
  }
}

