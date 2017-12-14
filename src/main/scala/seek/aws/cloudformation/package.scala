package seek.aws

import org.gradle.api.Project

package object cloudformation {

  object syntax extends HasCloudFormationPluginExtension.ToHasCloudFormationPluginExtensionOps

  object instances {
    implicit val projectHasCloudFormationPluginExtension = new HasCloudFormationPluginExtension[Project] {
      def cfnExt(p: Project) =
        p.getExtensions.getByType(classOf[CloudFormationPluginExtension])
    }
  }
}
