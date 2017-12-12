package seek.aws

import org.gradle.api.Project

package object lookup {

  object syntax extends HasLookupPluginExtension.ToHasLookupPluginExtensionOps

  object instances {
    implicit val projectHasLookupPluginExtension = new HasLookupPluginExtension[Project] {
      def lookupExt(p: Project) =
        p.getExtensions.getByType(classOf[LookupPluginExtension])
    }
  }
}
