package seek.aws
package cloudformation

import org.gradle.api.{Plugin, Project}

class CloudFormationPlugin extends Plugin[Project] {

  override def apply(project: Project): Unit = {
    project.getPlugins.apply(classOf[AwsPlugin])
    if (!project.getPlugins.hasPlugin(classOf[CloudFormationPlugin])) {
      project.getExtensions.create("cloudFormation", classOf[CloudFormationPluginExtension], project)
    }
    project.getTasks.create("createOrUpdateStack", classOf[CreateOrUpdateStack])
    project.getTasks.create("deleteStack", classOf[DeleteStack])
  }
}
