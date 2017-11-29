package seek

import org.gradle.api.Project

package object aws {

  object syntax extends HasAwsPluginExtension.ToHasAwsPluginExtensionOps

  object instances {
    implicit val projectHasAwsPluginExtension = new HasAwsPluginExtension[Project] {
      def awsExt(p: Project) =
        p.getExtensions.getByType(classOf[AwsPluginExtension])
    }
  }
}
