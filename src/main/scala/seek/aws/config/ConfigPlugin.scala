package seek.aws.config

import org.gradle.api.{Plugin, Project}

class ConfigPlugin extends Plugin[Project] {

  override def apply(project: Project): Unit =
    if (!project.getPlugins.hasPlugin(classOf[ConfigPlugin])) {
      project.getExtensions.create("config", classOf[ConfigPluginExtension], project)
    }
}
