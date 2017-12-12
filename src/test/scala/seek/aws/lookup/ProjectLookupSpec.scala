package seek.aws
package lookup

import java.io.File

import seek.aws.lookup.ProjectLookup.lookup

class ProjectLookupSpec extends SeekSpec {

  "A ProjectLookup" - {
    val projectProperties = Map("environment" -> "development", "region" -> "us-east-1")
    val configFiles = new TestFileCollection(Set(
      new File("src/test/resources/development.conf"),
      new File("src/test/resources/development.us-east-1.conf")))

    "when the default environment lookup key is used" - {
      val project = new TestProject(projectProperties)
      project.getExtensions.lookupExt.files(configFiles)

      "keys are looked up in a file matching the environment name" in {
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("camelCaseValue")
        lookup(project, "kebab-case-key").unsafeRunSync() should equal ("kebab-case-value")
        lookup(project, "dot.case.key").unsafeRunSync() should equal ("dot.case.value")
        lookup(project, "snake_case_key").unsafeRunSync() should equal ("snake_case_value")
      }
    }

    "when a multi-dimensional environment.region key is used" - {
      val project = new TestProject(projectProperties)
      project.getExtensions.lookupExt.files(configFiles)
      project.getExtensions.lookupExt.key("environment.region")

      "keys are looked up in a file matching the environment and region name" in {
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("regionSpecificCamelCaseValue")
      }
    }
  }
}
