# AWS Plugin for Gradle
This plugin provides a means of orchestrating the provisioning of AWS infrastructure and managing configuration parameters across environments.

## Quick Start

Apply the AWS and CloudFormation plugins to your Gradle project:

```
plugins {
    id 'seek.aws' version 'A.B.C'
    id 'seek.cloudformation' version 'A.B.C'
}
```

The latest versions of the `seek.aws` and `seek.cloudformation` plugins can be found [here](https://plugins.gradle.org/plugin/seek.aws) and [here](https://plugins.gradle.org/plugin/seek.cloudformation).

## The Plugins
This repository contains three logically separate but closely related Gradle plugins: `seek.aws`, `seek.config`, and `seek.cloudformation`.

### AWS Plugin
The AWS plugin must be applied to any Gradle project that wishes to use AWS-related tasks. This plugin will in turn apply the Config plugin which allows the AWS tasks to be configured lazily with configuration parameters that are resolved at runtime.

The AWS plugin is configured via a plugin extension named `aws`:

```
aws {
    region 'us-west-1'
    profile 'my-profile'
}
```

The methods of the `aws` extension are described below:

|Method   |Argument type  |Description            |Required|Default
|---------|---------------|-----------------------|--------|-------
|`region` |`String`       |AWS region             |Yes     |-
|`profile`|`String`       |AWS credentials profile|No      |"default"

The **`region`** method is used to specify the [AWS region](http://docs.aws.amazon.com/general/latest/gr/rande.html) that tasks will operate against by default. All AWS tasks also have a `region` method that takes precedence over the region specified in the `aws` extension. Tasks that do not specify a `region` fall back to the region defined in the `aws` extension. The `region` property is not technically required in `aws` but if it is not specified every AWS task must specify a region.

All AWS tasks use the [default credentials provider chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html). The **`profile`** method of `aws` can be used if you use named profiles in `~/.aws/credentials` otherwise it does not need to be specified.

The `aws` extension can use lookups to lazily set values via configuration. Lookups are described in the next section.

### Config Plugin
The Config plugin is applied by the AWS plugin so it does not need to be manually applied to Gradle projects that also apply `seek.aws`. This plugin allows AWS tasks to be configured with values that are resolved from configuration sources rather than needing to be hardcoded. Configuration sources can include Gradle project properties, closures, configuration files, [AWS Parameter Store](http://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-paramstore.html) and [CloudFormation stack outputs](http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/outputs-section-structure.html).

The Config plugin uses the [Lightbend config library](https://github.com/lightbend/config) (formerly Typesafe config) to parse configuration files.

The Config plugin is configured via a plugin extension named `config`:

```
config {
    files fileTree('config').include('*.conf')
}

```
The methods of the `config` extension are described below:

|Method                  |Argument type   |Description                               |Required|Default
|------------------------|----------------|------------------------------------------|--------|-------
|`naming`                |`String`        |Config file naming convention             |No      |"environment"
|`files`                 |`FileCollection`|Full set of configuration files           |No      |-
|`addFiles`              |`FileCollection`|Adds a set of configuration files         |No      |-
|`allowPropertyOverrides`|`Boolean`       |Whether project properties override config|No      |`true`
|`allowCommonConfig`     |`Boolean`       |Whether common config is allowed          |No      |`true`
|`commonConfigName`      |`String`        |Name of the common configuration file     |No      |"common"

The **`naming`** method specifies how configuration files are named. The default is "environment". This means that by default the value of "environment" will determine the name of the configuration file(s) to load. "environment" can be specified either as a parameter in AWS Parameter Store or a Gradle project property. If "environment" equals "production" the Config plugin will load any files named `production.conf`, `production.json`, and `production.properties`.

AWS Parameter Store provides a nice mechanism for resolution of the environment as this allows you to deploy your application without needing to specify which environment you are deploying to as the environment is determined by the AWS account you are authenticated against. If you use Gradle properties you'll need to remember to override the `environment` property depending on the environment you are deploying to.

The value of `naming` does not have to be one dimensional. For example, consider a service that is deployed to multiple regions within an environment. A possible naming scheme is "environment.region". At runtime the values of "environment" and "region" will be resolved to determine the name of the configuration files. If "environment" equals "production" and "region" equals "ap-southeast-2" then the plugin will look for files named `production.ap-southeast-2.(conf|json|properties)`.

The **`files`** method sets the `FileCollection` that contains all configuration files to be used. **`addFiles`** can be used to additively accumulate files. For example, in a multi-project Gradle build the root project might set top level common configuration files and subprojects might add their specific configuration files using `addFiles`. Configuration files must follow the (very flexible) [Lightbend config format](https://github.com/lightbend/config).

The **`allowPropertyOverrides`** method specifies whether Gradle project properties should be considered before looking up configuration files. This is useful for overriding configuration values on the command line. For example, if `allowPropertyOverrides` is `true` (the default) then:

```
gradle createOrUpdateStack -PbucketName=my-bucket
```

would use the value of `bucketName` specified on the command line rather than the value of `bucketName` found in configuration files or AWS Parameter Store.

The **`allowCommonConfig`** method specifies whether common configuration files are allowed. An application that is deployed to multiple environments will often have a number of configuration parameters that are common to all environments. When this value is true (the default) the plugin will load any configuration files that match **`commonConfigName`** and attempt to resolve parameters there if they can not be resolved in environment specific files.

#### Using Lookups
The Config plugin uses a "lookup" to represent an instruction to resolve a configuration key at runtime. Lookups can be specified as arguments to AWS task methods so that tasks are configured with different values depending on the environment, region, or any other dimension, they are running against.

Lookups are used by statically importing `seek.aws.config.Lookup.lookup`:

```
import static seek.aws.config.Lookup.lookup
```

The `lookup` method looks like:

**Scala signature:** `def lookup(key: String): Lookup`  
**Java signature:** `public static Lookup lookup(String key)`

The `lookup` method returns a `Lookup` object which is executed at task runtime. When a `Lookup` is run it will attempt to resolve the specified key first by using Gradle properties, then by using configuration files, and finally by using the AWS Parameter Store. Lookup resolution order is discussed in more detail in the next section.

All AWS tasks can make use of lookups. For example, consider the following task definition:

```
task uploadLambdaJar(type: UploadFile, dependsOn: shadowJar) {
    bucket lookup('buildBucket')
    key lambdaArtefactKey
    file shadowJar.archivePath
}
```
##### Other Types of Lookups

The `seek.aws.config.Lookup` class provides two other lookup methods. One for querying CloudFormation for the output key of a specified stack:

**Scala signature:** `def stackOutput(stackName: String, key: String): Lookup`  
**Java signature:** `public static Lookup stackOutput(String stackName, String key)`

And one for querying AWS Parameter Store directly without considering local configuration:

**Scala signature:** `def parameterStore(key: String): Lookup`  
**Java signature:** `public static Lookup parameterStore(String key)`

These auxilliary lookup methods can be used in the same fashion as `seek.aws.config.Lookup.lookup`.

#### Config Resolution
When the Config plugin attempts to resolve a lookup it will considers a number of sources: Gradle properties, then configuration files, and finally AWS Parameter Store.

Consider the following Gradle file snippet:

```scala
import static seek.aws.config.Lookup.lookup
import seek.aws.s3.UploadFile

ext {
    // Hardcoded for this example
    environment = 'development'
}

config {
    addFiles fileTree('config1').include('*')
    addFiles fileTree('config2').include('*')
}

task uploadLambdaJar(type: UploadFile, dependsOn: shadowJar) {
    bucket lookup('buildBucket')
    key lambdaArtefactKey
    file shadowJar.archivePath
}
```

The resolution of `buildBucket` is lazy meaning that it will take place when `uploadLambdaJar` runs not during Gradle's [initialisation phase](https://docs.gradle.org/current/userguide/build_lifecycle.html).

In the case of the above example using the `lookup` method the order of resolution is as follows:

1. Look for a project property named `buildBucket` (unless `config.allowPropertyOverrides` is set to `false`)
1. Look for a configuration key named `buildBucket`, `build-bucket`, `build.bucket`, or `build_bucket` in a file named `development.(conf|json|properties)` in directory `config3`
1. Look for a configuration key named `buildBucket`, `build-bucket`, `build.bucket`, or `build_bucket` in a file named `common.(conf|json|properties)` in directory `config3` (unless `config.allowCommonConfig` is set to `false`)
1. Look for a configuration key named `buildBucket`, `build-bucket`, `build.bucket`, or `build_bucket` in a file named `development.(conf|json|properties)` in directory `config2`
1. Look for a configuration key named `buildBucket`, `build-bucket`, `build.bucket`, or `build_bucket` in a file named `common.(conf|json|properties)` in directory `config2` (unless `config.allowCommonConfig` is set to `false`)
1. Look for a configuration key named `buildBucket`, `build-bucket`, `build.bucket`, or `build_bucket` in a file named `development.(conf|json|properties)` in directory `config1`
1. Look for a configuration key named `buildBucket`, `build-bucket`, `build.bucket`, or `build_bucket` in a file named `common.(conf|json|properties)` in directory `config1` (unless `config.allowCommonConfig` is set to `false`)

The resolution ends when the key is found or all sources are exhausted.

### CloudFormation Plugin
The CloudFormation plugin should be applied to projects that provision CloudFormation stacks.

The CloudFormation plugin is configured via a plugin extension named `cloudFormation`:

```
cloudFormation {
    stackName project.name
    templateFile file('src/main/cloudformation/application.yaml')
    policyFile file('src/main/cloudformation/policy.json')
    tags (['Owner', 'Project', 'Version'])
}
```

The methods of the `cloudFormation` extension are described below:

|Method                   |Argument type                       |Description                            |Required|Default
|-------------------------|------------------------------------|---------------------------------------|--------|-------
|`stackName`              |`String`                            |Stack name                             |Yes     |-
|`templateFile`           |`File`                              |Stack template file                    |Yes     |-
|`policyFile`             |`File`                              |Stack policy file                      |No      |No policy
|`parameters`             |`Map[String, Any]`                  |Stack parameters map                   |No      |Config driven
|`tags`                   |`Map[String, Any]` or `List[String]`|Stack tag map or tag name list         |No      |No tags
|`stackWaitTimeoutSeconds`|`Int`                               |Timeout for stack operations in seconds|No      |15 mins

The `stackName` property specifies the name for the CloudFormation stack. This property can be specified using a `lookup`, a Gradle property, or hard-coded. The plugin creates a task called `createOrUpdateStack` (detailed below). When this task is run it checks for the existence of a stack with this name - if it exists an update operation is performed; if it does not exist a create operation is performed.

The `templateFile` property specifies a `java.io.File` that references the YAML or JSON CloudFormation template for the stack. Similarly, the optional `policyFile` property specifies a stack policy file.

The `parameters` property can be used to specify a map of key-value pairs to be used as stack parameters. The map values can be hard-coded values or lookups. For example:

```
cloudFormation {
    ...
    parameters ([
        BuildBucket: lookup('buildBucket'),
        LambdaArtefactKey: "${buildPrefix}/${service}.jar",
        LambdaBatchSize: lookup('lambdaBatchSize'),
        TableName: stackOutput('scaffolding', 'TableName'),
        KinesisStream: parameterStore('eventStream')
    ])
    ...
}
```

The CloudFormation plugin resolves stack parameters by parsing the `Parameters` section of the template file and then attempts to resolve each parameter in the following order:

1. Project properties (unless `config.allowCommonConfig` is set to `false`)
1. Parameters map specified in `cloudFormation.parameters`
1. Configuration files

If all stack parameters are defined in configuration files or project properties then the `parameters` property of `cloudFormation` is unnecessary and not really recommended. It's main use is if you need to feed in parameter values from AWS Parameter Store or from a CloudFormation stack output.

The `tags` property can be specified as a map (in the same fashion as the `parameters` property) or as a list of tag keys. If a list is specified each tag key is looked up using the Config plugin.

The CloudFormation plugin applies the following tasks to the project:

|Task name            |Description
|---------------------|-----------
|`createOrUpdateStack`|Creates or updates the stack defined in the `cloudFormation` extension
|`deleteStack`        |Deletes the stack defined in the `cloudFormation` extension if it exists
|`verifyStack`        |Verifies that all stack parameters and tags specified in the template file are available and if they are prints them

## The Tasks

### S3 Tasks
### CloudFormation Tasks


## Examples
