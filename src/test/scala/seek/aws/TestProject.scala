package seek.aws

import java.io.File
import java.util.concurrent.Callable
import java.{lang, util}

import groovy.lang.Closure
import org.gradle.api._
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.file.{ConfigurableFileCollection, ConfigurableFileTree, CopySpec, DeleteSpec}
import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.normalization.InputNormalizationHandler
import org.gradle.process.{ExecSpec, JavaExecSpec}

import scala.collection.JavaConverters._
import scala.collection.mutable

class TestProject(val properties: mutable.Map[String, String] = mutable.Map.empty) extends Project {
  implicit val project = this

  val getProperties = properties.asJava
  val getExtensions = new TestExtensionContainer
  def hasProperty(propertyName: String) = properties.contains(propertyName)
  def property(propertyName: String) = properties(propertyName)

  def getNormalization = ???
  def container[T](`type`: Class[T]) = ???
  def container[T](`type`: Class[T], factory: NamedDomainObjectFactory[T]) = ???
  def container[T](`type`: Class[T], factoryClosure: Closure[_]) = ???
  def configurations(configureClosure: Closure[_]) = ???
  def copySpec(closure: Closure[_]) = ???
  def copySpec(action: Action[_ >: CopySpec]) = ???
  def copySpec() = ???
  def getStatus = ???
  def getChildProjects = ???
  def fileTree(baseDir: scala.Any) = ???
  def fileTree(baseDir: scala.Any, configureClosure: Closure[_]) = ???
  def fileTree(baseDir: scala.Any, configureAction: Action[_ >: ConfigurableFileTree]) = ???
  def fileTree(args: util.Map[String, _]) = ???
  def normalization(configuration: Action[_ >: InputNormalizationHandler]) = ???
  def getPath = ???
  def property[T](clazz: Class[T]) = ???
  def getRootDir = ???
  def getLogging = ???
  def mkdir(path: scala.Any) = ???
  def evaluationDependsOn(path: String) = ???
  def setVersion(version: scala.Any) = ???
  def getBuildDir = ???
  def getProviders = ???
  def getLogger = ???
  def findProperty(propertyName: String) = ???
  def evaluationDependsOnChildren() = ???
  def dependencies(configureClosure: Closure[_]) = ???
  def getRepositories = ???
  def setGroup(group: scala.Any) = ???
  def getAllprojects = ???
  def getDisplayName = ???
  def setProperty(name: String, value: scala.Any) = ???
  def getAnt = ???
  def getObjects = ???
  def subprojects(action: Action[_ >: Project]) = ???
  def subprojects(configureClosure: Closure[_]) = ???
  def getDepth = ???
  def findProject(path: String) = ???
  def getSubprojects = ???
  def setDescription(description: String) = ???
  def file(path: scala.Any) = ???
  def file(path: scala.Any, validation: PathValidation) = ???
  def getResources = ???
  def defaultTasks(defaultTasks: String*) = ???
  def copy(closure: Closure[_]) = ???
  def copy(action: Action[_ >: CopySpec]) = ???
  def getBuildscript = ???
  def absoluteProjectPath(path: String) = ???
  def getVersion = ???
  def getParent = ???
  def getDependencies = ???
  def zipTree(zipPath: scala.Any) = ???
  def uri(path: scala.Any) = ???
  def getGradle = ???
  def getTasksByName(name: String, recursive: Boolean) = ???
  def getProjectDir = ???
  def getName = ???
  def getAllTasks(recursive: Boolean) = ???
  def beforeEvaluate(action: Action[_ >: Project]) = ???
  def beforeEvaluate(closure: Closure[_]) = ???
  def project(path: String) = ???
  def project(path: String, configureClosure: Closure[_]) = ???
  def project(path: String, configureAction: Action[_ >: Project]) = ???
  def setBuildDir(path: File) = ???
  def setBuildDir(path: scala.Any) = ???
  def afterEvaluate(action: Action[_ >: Project]) = ???
  def afterEvaluate(closure: Closure[_]) = ???
  def repositories(configureClosure: Closure[_]) = ???
  def setDefaultTasks(defaultTasks: util.List[String]) = ???
  def getComponents = ???
  def artifacts(configureClosure: Closure[_]) = ???
  def artifacts(configureAction: Action[_ >: ArtifactHandler]) = ???
  def getRootProject = ???
  def tarTree(tarPath: scala.Any) = ???
  def sync(action: Action[_ >: CopySpec]) = ???
  def getBuildFile = ???
  def task(name: String) = ???
  def task(args: util.Map[String, _], name: String) = ???
  def task(args: util.Map[String, _], name: String, configureClosure: Closure[_]) = ???
  def task(name: String, configureClosure: Closure[_]) = ???
  def getConvention = ???
  def getGroup = ???
  def files(paths: AnyRef*) = ???
  def files(paths: scala.Any, configureClosure: Closure[_]) = ???
  def files(paths: scala.Any, configureAction: Action[_ >: ConfigurableFileCollection]) = ???
  def buildscript(configureClosure: Closure[_]) = ???
  def depthCompare(otherProject: Project) = ???
  def getProject = ???
  def exec(closure: Closure[_]) = ???
  def exec(action: Action[_ >: ExecSpec]) = ???
  def getArtifacts = ???
  def getDescription = ???
  def delete(paths: AnyRef*) = ???
  def delete(action: Action[_ >: DeleteSpec]) = ???
  def getDefaultTasks = ???
  def getLayout = ???
  def provider[T](value: Callable[T]) = ???
  def relativePath(path: scala.Any) = ???
  def createAntBuilder() = ???
  def allprojects(action: Action[_ >: Project]) = ???
  def allprojects(configureClosure: Closure[_]) = ???
  def getConfigurations = ???
  def ant(configureClosure: Closure[_]) = ???
  def ant(configureAction: Action[_ >: AntBuilder]) = ???
  def javaexec(closure: Closure[_]) = ???
  def javaexec(action: Action[_ >: JavaExecSpec]) = ???
  def configure(`object`: scala.Any, configureClosure: Closure[_]) = ???
  def configure(objects: lang.Iterable[_], configureClosure: Closure[_]) = ???
  def configure[T](objects: lang.Iterable[T], configureAction: Action[_ >: T]) = ???
  def relativeProjectPath(path: String) = ???
  def setStatus(status: scala.Any) = ???
  def getState = ???
  def getTasks = ???
  def compareTo(o: Project) = ???
  def getPlugins = ???
  def getPluginManager = ???
  def apply(closure: Closure[_]) = ???
  def apply(action: Action[_ >: ObjectConfigurationAction]) = ???
  def apply(options: util.Map[String, _]) = ???
  def dependencyLocking(x$1: org.gradle.api.Action[_ >: org.gradle.api.artifacts.dsl.DependencyLockingHandler]): Unit = ???
  def getDependencyLocking(): org.gradle.api.artifacts.dsl.DependencyLockingHandler = ???
}
