package seek.eng.aws

import com.amazonaws.regions.Region
import groovy.lang.Closure
import monix.eval.{Task => MTask}
import monix.execution.Scheduler.Implicits.global
import org.gradle.api.tasks.TaskAction
import org.gradle.api.{DefaultTask, GradleException, InvalidUserCodeException}

abstract class AwsTask extends DefaultTask {

  setGroup("AWS")

  @TaskAction
  def runInternal(): Unit =
    run.runSyncMaybe

  // TODO: Figure out a better way to express MTask - parameterise this class with an Effect type?
  protected def run: MTask[Unit]

  protected val logger = getLogger

  // TODO: Is there a better way to do this?
  protected def resolve(a: Any, name: String = ""): String = a match {
    case Some(x) => resolve(x, name)
    case c: Closure[_] => c.call().toString
    case x if x != None && x != null => x.toString
    case _ => userCodeError(s"Task property '${name}' must be set")
  }

  protected def region: Region = ???

  protected def userCodeError(m: String) =
    throw new InvalidUserCodeException(m)

  protected def genericError(m: String) =
    throw new GradleException(m)
}

