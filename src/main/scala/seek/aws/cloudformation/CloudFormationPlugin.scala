package seek.aws
package cloudformation

import org.gradle.api.{Plugin, Project}

class CloudFormationPlugin extends Plugin[Project] {

  override def apply(project: Project): Unit = {
    project.getPlugins.apply(classOf[AwsPlugin])
    project.getExtensions.create("cloudFormation", classOf[CloudFormationPluginExtension], project)
    project.getTasks.create("createOrUpdateStack", classOf[CreateOrUpdateStack])
    project.getTasks.create("deleteStack", classOf[DeleteStack])
    project.getTasks.create("verifyStack", classOf[VerifyStack])
  }
}
