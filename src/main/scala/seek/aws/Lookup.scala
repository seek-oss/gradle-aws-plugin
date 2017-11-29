package seek.aws

import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import org.gradle.api._
import seek.aws.Lookup.MissingPropertyException

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import seek.aws.syntax._
import seek.aws.instances._


sealed trait Lookup {
  def run(p: Project): String
}

object Lookup {

  class MissingPropertyException(p: String) extends GradleException(s"Property '${p}' expected but not defined")

  def lookup(key: String): Lookup =
    new PropertyLookup(key)

  def stackOutput(stackName: String, key: String): Lookup =
    new CloudFormationStackOutputLookup(stackName, key)
}

class CloudFormationStackOutputLookup(stackName: String, key: String) extends Lookup {
  def run(p: Project): String = {
    val region = p.awsExt.region.get
//    val c = AmazonCloudFormationClientBuilder.standard().withRegion()
    ""
  }
}

class PropertyLookup(key: String) extends Lookup {

  def run(p: Project): String =
    property(p, key) match {
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

private object Test extends App {

//  val props1 = Map(
//    "environment"     -> "development",
//    "development.foo" -> 51
//  ).asJava
//  val x = Lookup.lookup("foo").get[Int](props1, "environment")
//  println(x)
//
//  val props2 = Map(
//    "application"               -> "projector",
//    "environment"               -> "development",
//    "projector.development.foo" -> 52
//  ).asJava
//   val y = Lookup.lookup("foo").get[Int](props2, "application.environment")
//  println(y)
}

