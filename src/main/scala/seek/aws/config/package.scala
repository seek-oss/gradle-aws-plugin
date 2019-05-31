package seek.aws

import org.gradle.api.Project

package object config {
  object instances {
    implicit class projectHasConfigPluginExtension(p: Project) {
      def cfgExt =
        p.getExtensions.getByType(classOf[ConfigPluginExtension])
    }
  }
}
