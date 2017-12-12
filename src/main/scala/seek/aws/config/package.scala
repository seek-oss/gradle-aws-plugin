package seek.aws

import org.gradle.api.Project

package object config {

  object syntax extends HasConfigPluginExtension.ToHasConfigPluginExtensionOps

  object instances {
    implicit val projectHasConfigPluginExtension = new HasConfigPluginExtension[Project] {
      def cfgExt(p: Project) =
        p.getExtensions.getByType(classOf[ConfigPluginExtension])
    }
  }
}
