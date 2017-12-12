package seek.aws

import java.io.File

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec

import scala.collection.JavaConverters._

class TestFileCollection(files: Set[File]) extends FileCollection {

  def getFiles = files.asJava

  def minus(fileCollection: FileCollection) = ???
  def asType(aClass: Class[_]) = ???
  def getAsFileTree = ???
  def add(fileCollection: FileCollection) = ???
  def isEmpty = ???
  def addToAntBuilder(o: scala.Any, s: String, antType: FileCollection.AntType) = ???
  def addToAntBuilder(o: scala.Any, s: String) = ???
  def plus(fileCollection: FileCollection) = ???
  def filter(closure: Closure[_]) = ???
  def filter(spec: Spec[_ >: File]) = ???
  def contains(file: File) = ???
  def getSingleFile = ???
  def stopExecutionIfEmpty() = ???
  def getAsPath = ???
  def iterator() = ???
  def getBuildDependencies = ???
}
