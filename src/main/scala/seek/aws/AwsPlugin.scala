package seek.aws

import org.gradle.api.{Plugin, Project}
import seek.aws.lookup.LookupPlugin

class AwsPlugin extends Plugin[Project] {

  override def apply(project: Project): Unit = {
    project.getPlugins.apply(classOf[LookupPlugin])
    if (!project.getPlugins.hasPlugin(classOf[AwsPlugin])) {
      project.getExtensions.create("aws", classOf[AwsPluginExtension], project)
    }
  }
}
