package seek.aws

import org.gradle.api.{Action, Project}
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.reflect.TypeOf
import seek.aws.cloudformation.CloudFormationPluginExtension

class TestExtensionContainer(implicit project: Project) extends ExtensionContainer {

  val aws = new AwsPluginExtension
  val cfn = new CloudFormationPluginExtension

  def getByType[T](t: Class[T]) = {
    val awsExtClass = classOf[AwsPluginExtension]
    val cfnExtClass = classOf[CloudFormationPluginExtension]
    t match {
      case `awsExtClass` => aws.asInstanceOf[T]
      case `cfnExtClass` => cfn.asInstanceOf[T]
      case _             => throw new Exception(s"Unexpected extension type: ${t}")
    }
  }

  def findByType[T](t: Class[T]) = ???
  def add[T](publicType: Class[T], name: String, extension: T) = ???
  def add[T](publicType: TypeOf[T], name: String, extension: T) = ???
  def add(name: String, extension: scala.Any) = ???
  def getByType[T](t: TypeOf[T]) = ???
  def getExtraProperties = ???
  def findByName(name: String) = ???
  def configure[T](t: Class[T], action: Action[_ >: T]) = ???
  def configure[T](t: TypeOf[T], action: Action[_ >: T]) = ???
  def configure[T](name: String, action: Action[_ >: T]) = ???
  def getSchema = ???
  def getByName(name: String) = ???
  def create[T](publicType: Class[T], name: String, instanceType: Class[_ <: T], constructionArguments: AnyRef*) = ???
  def create[T](publicType: TypeOf[T], name: String, instanceType: Class[_ <: T], constructionArguments: AnyRef*) = ???
  def create[T](name: String, t: Class[T], constructionArguments: AnyRef*) = ???
  def findByType[T](t: TypeOf[T]) = ???
}