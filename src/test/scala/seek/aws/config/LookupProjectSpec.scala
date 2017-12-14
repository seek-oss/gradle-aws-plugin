package seek.aws
package config

import java.io.File
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.{Files, Paths}

import seek.aws.config.LookupProject.lookup

import scala.collection.mutable

class LookupProjectSpec extends SeekSpec {

  "A ProjectLookup" - {

    "when the default environment lookup index is used" - {
      val project = buildGradleProject

      "keys are looked up in a file matching the environment name" in {
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("camelCaseValue")
        lookup(project, "kebab-case-key").unsafeRunSync() should equal ("kebab-case-value")
        lookup(project, "dot.case.key").unsafeRunSync() should equal ("dot.case.value")
        lookup(project, "snake_case_key").unsafeRunSync() should equal ("snake_case_value")
      }
    }

    "when a multi-dimensional environment.region index is used" - {
      val project = buildGradleProject
      project.getExtensions.lookupExt.naming("environment.region")

      "keys are looked up in a file matching the environment and region name" in {
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("regionSpecificCamelCaseValue")
      }
    }

    "when a key is present as project property" - {
      val project = buildGradleProject
      project.properties.put("camelCaseKey", "overriddenValue")

      "its value overrides any configuration values for the same key by default" in {
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("overriddenValue")
      }

      "but its value is not used if allowProjectOverrides is disabled" in {
        project.getExtensions.lookupExt.allowProjectOverrides = false
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("camelCaseValue")
      }

      "when an underride is specified to the lookup operation" - {
        val project = buildGradleProject

        "its value overrides any configuration values for the same key" in {
          lookup(project, "camelCaseKey", Map("camelCaseKey" -> "underriddenValue"))
            .unsafeRunSync() should equal ("underriddenValue")
        }

        "but its value is overridden by project properties" in {
          project.properties.put("camelCaseKey", "overriddenValue")
          lookup(project, "camelCaseKey", Map("camelCaseKey" -> "underriddenValue"))
            .unsafeRunSync() should equal ("overriddenValue")
        }
      }
    }

    "when the same Gradle project is used for multiple configuration lookups" - {
      val tempConfigFile = new File("build/tmp/tests/development.conf")
      tempConfigFile.getParentFile.mkdirs()
      Files.copy(Paths.get("src/test/resources/development.conf"), tempConfigFile.toPath, REPLACE_EXISTING)
      val project = buildGradleProject
      project.getExtensions.lookupExt.files(new TestFileCollection(Set(tempConfigFile)))

      "the configuration set is cached and not re-built after the first lookup" in {
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("camelCaseValue")
        Files.write(tempConfigFile.toPath, "camelCaseKey = newCamelCaseValue".getBytes)
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("camelCaseValue")
      }
     }

    "when there are multiple configuration files with the same" - {
      val project = buildGradleProject
      project.getExtensions.lookupExt.files(new TestFileCollection(Set()))
      project.getExtensions.lookupExt.addFiles(new TestFileCollection(Set(new File("src/test/resources/development.conf"))))
      project.getExtensions.lookupExt.addFiles(new TestFileCollection(Set(new File("src/test/resources/subproject/development.conf"))))

      "configuration files are layered in LIFO order" in {
        lookup(project, "camelCaseKey").unsafeRunSync() should equal ("subprojectSpecificCamelCaseValue")
        lookup(project, "kebab-case-key").unsafeRunSync() should equal ("kebab-case-value")
      }
    }

    "when the lookup key cannot be found" - {
      val project = buildGradleProject

      "the operation fails with a LookupProjectFailed exception" in {
        val thrown = intercept[LookupKeyNotFound](lookup(project, "nonExistentKey").unsafeRunSync())
        thrown.key should equal ("nonExistentKey")
      }
    }
  }

  def buildGradleProject: TestProject = {
    val projectProperties = mutable.Map("environment" -> "development", "region" -> "us-east-1")
    val configFiles = new TestFileCollection(Set(
      new File("src/test/resources/development.conf"),
      new File("src/test/resources/development.us-east-1.conf")))
    val project = new TestProject(projectProperties)
    project.getExtensions.lookupExt.files(configFiles)
    project
  }
}
