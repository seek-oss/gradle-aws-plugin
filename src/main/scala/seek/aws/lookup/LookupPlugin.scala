package seek.aws.lookup

import org.gradle.api.{Plugin, Project}

class LookupPlugin extends Plugin[Project] {

  override def apply(project: Project): Unit =
    if (!project.getPlugins.hasPlugin(classOf[LookupPlugin])) {
      project.getExtensions.create("lookupConfig", classOf[LookupPluginExtension], project)
    }
}
