package seek.aws

import org.gradle.api.Project

package object config {

  object syntax extends HasConfigPluginExtension.ToHasConfigPluginExtensionOps

  object instances {
    implicit val projectHasConfigPluginExtension = new HasConfigPluginExtension[Project] {
      def configExt(p: Project) =
        p.getExtensions.getByType(classOf[ConfigPluginExtension])
    }
  }
}
