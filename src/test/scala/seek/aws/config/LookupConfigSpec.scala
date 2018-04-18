package seek.aws
package config

import java.io.File
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.{Files, Paths}

import org.gradle.api.GradleException

import scala.collection.mutable

class LookupConfigSpec extends SeekSpec {

  "A ProjectLookup" - {

    "when the default environment lookup index is used" - {
      val project = buildGradleProject

      "keys are looked up in a file matching the environment name" in {
        LookupConfig("camelCaseKey").run(project).unsafeRunSync() should equal ("camelCaseValue")
        LookupConfig("kebab-case-key").run(project).unsafeRunSync() should equal ("kebab-case-value")
        LookupConfig("dot.case.key").run(project).unsafeRunSync() should equal ("dot.case.value")
        LookupConfig("snake_case_key").run(project).unsafeRunSync() should equal ("snake_case_value")
      }
    }

    "when a multi-dimensional environment.region index is used" - {
      val project = buildGradleProject
      project.getExtensions.lookupExt.naming("environment.region")

      "keys are looked up in a file matching the environment and region name" in {
        LookupConfig("camelCaseKey").run(project).unsafeRunSync() should equal ("regionSpecificCamelCaseValue")
      }
    }

    "when the same Gradle project is used for multiple configuration lookups" - {
      val tempConfigFile = new File("build/tmp/tests/development.conf")
      tempConfigFile.getParentFile.mkdirs()
      Files.copy(Paths.get("src/test/resources/development.conf"), tempConfigFile.toPath, REPLACE_EXISTING)
      val project = buildGradleProject
      project.getExtensions.lookupExt.files(new TestFileCollection(Set(tempConfigFile)))

      "the configuration set is cached and not re-built after the first lookup" in {
        LookupConfig("camelCaseKey").run(project).unsafeRunSync() should equal ("camelCaseValue")
        Files.write(tempConfigFile.toPath, "camelCaseKey = newCamelCaseValue".getBytes)
        LookupConfig("camelCaseKey").run(project).unsafeRunSync() should equal ("camelCaseValue")
      }
     }

    "when there are multiple configuration files with the same" - {
      val project = buildGradleProject
      project.getExtensions.lookupExt.files(new TestFileCollection(Set()))
      project.getExtensions.lookupExt.addFiles(new TestFileCollection(Set(new File("src/test/resources/development.conf"))))
      project.getExtensions.lookupExt.addFiles(new TestFileCollection(Set(new File("src/test/resources/subproject/development.conf"))))

      "configuration files are layered in LIFO order" in {
        LookupConfig("helloKey").run(project).unsafeRunSync() should equal ("helloMate")
        LookupConfig("camelCaseKey").run(project).unsafeRunSync() should equal ("subprojectSpecificCamelCaseValue")
        LookupConfig("kebab-case-key").run(project).unsafeRunSync() should equal ("kebab-case-value")
      }
    }

    "when a common config is used with an environment config and the same key exists in both files" - {
      val project = buildGradleProject
      project.getExtensions.lookupExt.files(new TestFileCollection(Set(
        new File("src/test/resources/common.conf"),
        new File("src/test/resources/development.conf")
      )))

      "environment file takes precedence for values with the same key" in {
        LookupConfig("kebab-case-key").run(project).unsafeRunSync() should equal ("kebab-case-value")
        LookupConfig("camelCaseKey").run(project).unsafeRunSync() should equal ("camelCaseValue")
        LookupConfig("hello").run(project).unsafeRunSync() should equal ("mate")
      }
    }

    "when the lookup key cannot be found" - {
      val project = buildGradleProject

      "run fails with a GradleException" in {
        val thrown = intercept[GradleException](LookupConfig("nonExistentKey").run(project).unsafeRunSync())
      }

      "runOptional returns None" in {
        LookupConfig("nonExistentKey").runOptional(project).value.unsafeRunSync() should equal (None)
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
