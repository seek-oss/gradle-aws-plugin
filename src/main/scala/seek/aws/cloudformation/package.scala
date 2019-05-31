package seek.aws

import org.gradle.api.Project

package object cloudformation {
  object instances {
    implicit class ProjectHasCloudFormationPluginExtension(p: Project) {
      def cfnExt =
        p.getExtensions.getByType(classOf[CloudFormationPluginExtension])
    }
  }
}
