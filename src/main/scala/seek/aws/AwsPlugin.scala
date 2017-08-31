package seek.aws

import org.gradle.api.{Plugin, Project}

class AwsPlugin extends Plugin[Project] {

  override def apply(project: Project): Unit =
    if (!project.getPlugins.hasPlugin(classOf[AwsPlugin])) {
      project.getExtensions.create("aws", classOf[AwsPluginExtension])
    }
}
