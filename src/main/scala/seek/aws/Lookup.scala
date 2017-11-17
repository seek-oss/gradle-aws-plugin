package seek.aws

import org.gradle.api._
import seek.aws.Lookup.MissingPropertyException

import scala.annotation.tailrec
import scala.collection.JavaConverters._

object Lookup {

  class MissingPropertyException(p: String) extends GradleException(s"Property '${p}' expected but not defined")

  def lookup(key: String): LookupProperty =
    new LookupProperty(key)
}

private[aws] class LookupProperty(key: String) {

  def get[A](props: ProjectProps, lookupPrefix: String): A = {
    val fullKey = buildKey(props, lookupPrefix.split('.').toList)
    property(props, fullKey).getOrElse(throw new MissingPropertyException(fullKey)).asInstanceOf[A]
  }

  @tailrec
  private def buildKey(props: ProjectProps, prefixParts: List[String], acc: String = ""): String =
    prefixParts match {
      case h :: t =>
        property(props, h) match {
          case Some(v: String) if v.nonEmpty => buildKey(props, t, acc + v + ".")
          case _ => throw new MissingPropertyException(h)
        }
      case _ => acc + key
    }

  private def property(props: ProjectProps, key: String): Option[Any] =
    props.asScala.get(key) match {
      case Some(v) => Some(v)
      case _       => None
    }
}

private object Test extends App {

  val props1 = Map(
    "environment"     -> "development",
    "development.foo" -> 51
  ).asJava
  val x = Lookup.lookup("foo").get[Int](props1, "environment")
  println(x)

  val props2 = Map(
    "application"               -> "projector",
    "environment"               -> "development",
    "projector.development.foo" -> 52
  ).asJava
   val y = Lookup.lookup("foo").get[Int](props2, "application.environment")
  println(y)
}

