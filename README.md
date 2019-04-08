# AWS Plugin for Gradle
This plugin provides a means of orchestrating the provisioning of AWS infrastructure and managing configuration parameters across environments.

## Table of Contents

  * [Quick Start](#quick-start)
  * [Example](#full-example)
  * [The Plugins](#the-plugins)
     * [AWS Plugin](#aws-plugin)
     * [Config Plugin](#config-plugin)
        * [Using Lookups](#using-lookups)
        * [Config Resolution](#config-resolution)
     * [CloudFormation Plugin](#cloudformation-plugin)
  * [The Tasks](#the-tasks)
     * [S3 Tasks](#s3-tasks)
     * [CloudFormation Tasks](#cloudformation-tasks)
     * [Simple Systems Manager Tasks](#simple-systems-manager-tasks)

## Quick Start

Apply the AWS and CloudFormation plugins to your Gradle project:

```gradle
plugins {
    id 'seek.aws' version 'A.B.C'
    id 'seek.cloudformation' version 'A.B.C'
}
```

The latest versions of the `seek.aws` and `seek.cloudformation` plugins can be found [here](https://plugins.gradle.org/plugin/seek.aws) and [here](https://plugins.gradle.org/plugin/seek.cloudformation).

## Example

A full working example can be found in the [example](example) directory.

## The Plugins
This repository contains three logically separate but closely related Gradle plugins: `seek.aws`, `seek.config`, and `seek.cloudformation`.

### AWS Plugin
The AWS plugin must be applied to any Gradle project that wishes to use AWS-related tasks. This plugin will in turn apply the Config plugin which allows the AWS tasks to be configured lazily with configuration parameters that are resolved at runtime.

The AWS plugin is configured via a plugin extension named `aws`:

```gradle
aws {
    region 'us-west-1'
}
```

The methods of the `aws` extension are described below:

|Method   |Argument type  |Description            |Required|Default
|---------|---------------|-----------------------|--------|-------
|`region` |`String`       |AWS region             |No      |-
|`roleArn`|`String`       |IAM Role to assume     |No      |-

The **`region`** method is used to specify the [AWS region](http://docs.aws.amazon.com/general/latest/gr/rande.html) that tasks will operate against by default. All AWS tasks also have a `region` method that takes precedence over the region specified in the `aws` extension. Tasks that do not specify a `region` fall back to the region defined in the `aws` extension. `region` is not technically required but is recommended to avoid accidentally deploying your application to the wrong region.

The **`roleArn`** method can be used to specify the ARN of an IAM role to [assume](http://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html) when running AWS tasks. All AWS tasks also have a `roleArn` method that takes precedence over the role specified in the `aws` extension. If no role is specified the [default credentials provider chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html) will be used.

### Config Plugin
The Config plugin is applied by the AWS plugin so it does not need to be manually applied to Gradle projects that also apply `seek.aws`. This plugin allows AWS tasks to be configured with values that are resolved from configuration sources rather than needing to be hardcoded. Configuration sources can include Gradle project properties, closures, configuration files, [AWS Parameter Store](http://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-paramstore.html) and [CloudFormation stack outputs](http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/outputs-section-structure.html).

The Config plugin uses the [Lightbend config library](https://github.com/lightbend/config) (formerly Typesafe config) to parse configuration files.

The Config plugin is configured via a plugin extension named `config`:

```gradle
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

The **`naming`** method specifies how configuration files are named. The default is "environment". This means that by default the value of "environment" will determine the name of the configuration file(s) to load. The environment can be specified either as a parameter in AWS Parameter Store or a Gradle project property. If the environment is set to "production" the Config plugin will load any files named `production.conf`, `production.json`, and `production.properties`.

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

**Scala signature:**

```scala
def lookup(key: String): Lookup
```

**Java signature:**

```java
public static Lookup lookup(String key)
```

The `lookup` method returns a `Lookup` object which is executed at task runtime. When a `Lookup` is run it will attempt to resolve the specified key first by using Gradle properties, then by using configuration files, and finally by using the AWS Parameter Store. Lookup resolution order is discussed in more detail in the next section.

All AWS tasks can make use of lookups. For example, consider the following task definition:

```gradle
task uploadLambdaJar(type: UploadFile, dependsOn: shadowJar) {
    bucket lookup('buildBucket')
    key lambdaArtefactKey
    file shadowJar.archivePath
}
```
##### Other Types of Lookups

The `seek.aws.config.Lookup` class provides two other lookup methods. One for querying CloudFormation for the output key of a specified stack:

**Scala signature:**

```scala
def stackOutput(stackName: String, key: String): Lookup
```

**Java signature:**

```java
public static Lookup stackOutput(String stackName, String key)
```

And one for querying AWS Parameter Store directly without considering local configuration:

**Scala signature:**

```scala
def parameterStore(key: String): Lookup
```

**Java signature:**

```java
public static Lookup parameterStore(String key)
```

These auxilliary lookup methods can be used in the same fashion as `seek.aws.config.Lookup.lookup`.

#### Config Resolution
When the Config plugin attempts to resolve a lookup it will considers a number of sources: Gradle properties, then configuration files, and finally AWS Parameter Store.

Consider the following Gradle file snippet:

```gradle
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

In the case of the above example, at runtime the Config plugin will look for:

1. A project property named `buildBucket`
1. A `buildBucket` configuration key in `development.(conf|json|properties)` in directory `config2`
1. A `buildBucket` configuration key in `common.(conf|json|properties)` in directory `config2`
1. A `buildBucket` configuration key in `development.(conf|json|properties)` in directory `config1`
1. A `buildBucket` configuration key in `common.(conf|json|properties)` in directory `config1`
1. A `buildBucket` parameter key in the AWS Parameter Store

The resolution ends when the key is found or all sources are exhausted.

**Note:** In steps 2-5 when the Config plugin attempts to resolve `buildBucket` via configuration files the Plugin will attempt to look for the following variations of the key name in order of: `buildBucket`, `build-bucket`, `build.bucket`, `build_bucket`.


### CloudFormation Plugin
The CloudFormation plugin should be applied to projects that provision CloudFormation stacks.

The CloudFormation plugin is configured via a plugin extension named `cloudFormation`:

```gradle
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

The **`stackName`** method specifies the name for the CloudFormation stack. The name can be specified using a `lookup`, a Gradle property, or hardcoded. The CloudFormation plugin creates a task named `createOrUpdateStack` (described below) that when run checks for the existence of a stack with this name - if it exists an update operation is performed; if it does not exist a create operation is performed.

The **`templateFile`** method specifies a `java.io.File` that references the YAML or JSON CloudFormation template for the stack. Similarly, the optional **`policyFile`** method specifies a stack policy file.

The **`parameters`** property can be used to specify a map of key-value pairs to be used as stack parameters. The map values can be hard-coded values or lookups. For example:

```gradle
cloudFormation {
    //...
    parameters ([
        BuildBucket: lookup('buildBucket'),
        LambdaArtefactKey: "${buildPrefix}/${service}.jar",
        LambdaBatchSize: lookup('lambdaBatchSize'),
        TableName: stackOutput('scaffolding', 'TableName'),
        KinesisStream: parameterStore('eventStream')
    ])
    //...
}
```

The CloudFormation plugin resolves stack parameters by parsing the `Parameters` section of the template file and then using the Config plugin to resolve each parameter. The CloudFormation plugin expects the CloudFormation parameters in the template file to be specified in `PascalCase`. It then uses the Config plugin to resolve the `camelCase` version of each parameter. As described previously the Config plugin is quite lenient when looking up keys in configuration files and will try multiple case variations.

CloudFormation template parameters are resolved in the following order:

1. Project properties (unless `config.allowPropertyOverrides` is set to `false`)
1. Optional map specified to `cloudFormation.parameters`
1. Configuration files
1. AWS Parameter Store

If all stack parameters are defined in any combination of Gradle project properties, configuration files, and AWS Parameter Store then the `parameters` property of `cloudFormation` is not necessary.

The `tags` property can be specified as a map (in the same fashion as the `parameters` property) or as a list of tag keys. If a list is specified each tag key is looked up using the Config plugin.

The CloudFormation plugin applies the following tasks to the project:

|Task name            |Description
|---------------------|-----------
|`createOrUpdateStack`|Creates or updates the stack defined in the `cloudFormation` extension
|`deleteStack`        |Deletes the stack defined in the `cloudFormation` extension if it exists
|`verifyStack`        |Verifies that all stack parameters and tags specified in the template file are available and if they are prints them

When running gradle with `--info` or `-i` the plugin will log CloudFormation stack events. This is especially useful for logfiles in CI builds. Here is an example:

```
$ ./gradlew :service1:createOrUpdateStack -i
[...]
> Task :service1:createOrUpdateStack
Task ':service1:createOrUpdateStack' is not up-to-date because:
  Task has not declared any outputs despite executing actions.
2018-11-03 17:33:35 UPDATE_IN_PROGRESS                  AWS::CloudFormation::Stack               service1                       User Initiated
2018-11-03 17:33:42 UPDATE_IN_PROGRESS                  AWS::ECS::TaskDefinition                 TaskDefinition                 Requested update requires the creation of a new physical resource; hence creating one.
2018-11-03 17:33:42 UPDATE_IN_PROGRESS                  AWS::ECS::TaskDefinition                 TaskDefinition                 Resource creation Initiated
2018-11-03 17:33:42 UPDATE_COMPLETE                     AWS::ECS::TaskDefinition                 TaskDefinition
2018-11-03 17:33:44 UPDATE_IN_PROGRESS                  AWS::ECS::Service                        Service
[...]
```

## The Tasks

This section provides an overview of the AWS tasks available.

### S3 Tasks

#### `UploadFile`

The `seek.aws.s3.UploadFile` task uploads a single file, with optional interpolation, to an S3 bucket with a specified key.

**Example:**

```gradle
task upload(type: UploadFile) {
    file file('build.gradle')
    bucket lookup('bucketName')
    key 'uploads/build.gradle'
}
```

|Method              |Argument type|Description                                  |Required|Default
|--------------------|-------------|---------------------------------------------|--------|-------
|`file`              |`File`       |File to upload                               |Yes     |-
|`bucket`            |`String`     |S3 bucket name                               |Yes     |-
|`key`               |`String`     |S3 key                                       |Yes     |-
|`acl`               |`String`     |Access Control List to apply to file         |No      |-
|`failIfObjectExists`|`Boolean`    |Whether to fail fast if object already exists|No      |`false`
|`interpolate`       |Various      |Interpolation rules described below          |No      |-

#### `UploadFiles`

The `seek.aws.s3.UploadFile` task uploads a single file, with optional interpolation, to an S3 bucket with a specified key.

**Example:**

```gradle
task upload(type: UploadFiles) {
    files fileTree('config').include('*')
    bucket lookup('bucketName')
    prefix 'configs'
}
```

|Method                   |Argument type   |Description                                              |Required|Default
|-------------------------|----------------|---------------------------------------------------------|--------|-------
|`files`                  |`FileCollection`|Files to upload                                          |Yes     |-
|`bucket`                 |`String`        |S3 bucket name                                           |Yes     |-
|`prefix`                 |`String`        |S3 prefix                                                |Yes     |-
|`acl`                    |`String`        |Access Control List to apply to file                     |No      |-
|`failIfObjectExists`     |`Boolean`       |Whether to fail fast if an object already exists         |No      |`false`
|`failIfPrefixExists`     |`Boolean`       |Whether to fail fast if the prefix already exists        |No      |`false`
|`cleanPrefixBeforeUpload`|`Boolean`       |Whether to delete all files in the prefix prior to upload|No      |`false`
|`interpolate`            |Various         |Interpolation rules described below                      |No      |-

#### Interpolation

Both `UploadFile` and `UploadFiles` support interpolation of files prior to upload. Interpolation leverages the Config plugin to resolve tokenised keys to values.

Shown below is an `UploadFiles` task that interpolates all files using the Config plugin to resolve configuration keys.

```gradle
task upload(type: UploadFiles) {
    files fileTree('out').include('*')
    bucket lookup('bucketName')
    prefix 'files'
    interpolate true
}
```

By default interpolation uses the start token `{{{` and the end token `}}}`. So in the example above, if a file contained the line:

```
The {{{animal}}} jumps over the {{{otherAnimal}}}
```

the keys `animal` and `otherAnimal` would be resolved using the Config plugin and would be substituted into a copy of the file (stored in the `build` directory) prior to upload. If a key cannot be found the upload task fails and prints the name of the unresolved key.

Below is an example of a call to `interpolate` which overrides the default start and end tokens:

```gradle
interpolate(true) {
    startToken = '${'
    endToken = '}'
}
```

Below is an example of a call to `interpolate` which only interpolates a single file (within the set being uploaded) and provides a map of interpolation key-values. Any interpolation keys not found within this map will fall back to values resolved by the Config plugin.

```gradle
interpolate(file('out/story.sh')) {
    replace = [animal: 'quick brown fox', otherAnimal: 'lazy dog']
}
```

To see all the `interpolate` overrides see the source [here](src/main/scala/seek/aws/s3/Upload.scala).


### CloudFormation Tasks

#### `CreateOrUpdateStack`

The `seek.aws.cloudformation.CreateOrUpdateStack` task is created with the name **`createOrUpdateStack`** when the CloudFormation plugin is applied to a Gradle project. This task is configured with the values of the `cloudFormation` extension as described in the CloudFormation plugin section.

#### `VerifyStack`

The `seek.aws.cloudformation.CreateOrUpdateStack` task is created with the name **`verifyStack`** when the CloudFormation plugin is applied to a Gradle project. This task verifies that all stack parameter values and tag values can be resolved. If they can be resolved the key value pairs are logged at the `lifecycle` log level. If any parameters or tags cannot be resolved the task fails with an exception that describes the offending configuration key.

This task is useful during development cycle when you wish to see the parameters and tags that the stack will be created with without going through a full deployment.

#### `DeleteStack`

The `seek.aws.cloudformation.DeleteStack` task is created with the name **`deleteStack`** when the CloudFormation plugin is applied to a Gradle project. This task provides a means of deleting the stack specified by the `cloudFormation` extension block. If the stack is not present then the task exits quietly.

#### `DeleteStacks`

The `seek.aws.cloudformation.DeleteStacks` task can be used to delete one or more stacks that match a specified regex. By default this task is configured with a safety switch that will only allow a maximum of 3 stacks to be deleted. This is to prevent accidental deletion of all stacks in your account. The safety switch and the limit can be configured at your own risk!

**Example:**

```gradle
task tearDown(type: DeleteStacks) {
    nameMatching 'my-app-.*'
}
```

|Method        |Argument type|Description                                                 |Required|Default
|--------------|-------------|------------------------------------------------------------|--------|-------
|`nameMatching`|`String`     |Regex that is matched against all stacks in the region      |Yes     |-
|`excluding`   |`String`     |Stack to exclude in list returned by `nameMatching`         |No      |-
|`safetyOn`    |`Boolean`    |Whether the safety switch is on                             |No      |`true`
|`safetyLimit` |`Integer`    |Maximum number of stacks that can be deleted if safety is on|No      |3


### Simple Systems Manager Tasks

#### `PutParameters`

The `seek.aws.ssm.PutParameters` task can be used to upload one or more parameters to AWS Parameter Store.

**Example:**

```gradle
task putParameters(type: PutParameters) {
    parameter('vpcId') {
        value = stackOutput('account', 'VpcId')
        type = 'String'
        description = 'ID of the primary VPC'
    }
    parameter('artifactoryPassword') {
        value = lookup('artifactoryPassword')
        type = 'SecureString'
        description = 'Password for Artifactory'
    }
}
```

The `PutParameters` task can upload an arbitrary number of parameters. Each parameter is added by calling the `parameter` method with the name of the parameter (this can be hard-coded or a lookup) and a closure that configures the parameter. The configuration closure can set the following properties (which can also be hard-coded or use lookups):

|Property        |Argument type|Description                                      |Required|Default
|----------------|-------------|-------------------------------------------------|--------|-------
|`value`         |`String`     |Value of the parameter                           |Yes     |-
|`type`          |`String`     |Either "String", "StringList", or "SecureString" |Yes     |-
|`description`   |`String`     |Description of the parameter                     |Yes     |-
|`keyId`         |`String`     |KMS key ID of the key to use to encrypt the value|No      |-
|`overwrite`     |`String`     |Whether overwriting is allowed                   |No      |true
|`allowedPattern`|`String`     |Allowed pattern regex for the value              |No      |-

For more details on the `put-parameters` AWS API call see [here](http://docs.aws.amazon.com/cli/latest/reference/ssm/put-parameter.html).

### Simple Notification Service Tasks

#### `SubscribeTopic`

The `seek.aws.sns.SubscribeTopic` task can be used to add one or more subscriptions to an SNS topic if they do not already exist.

**Example:**

```gradle
task subscribeTopic(type: SubscribeTopic) {
    topicArn lookup('alertTopic')
    subscribe('email', 'alerts@example.com')
    subscribe(lookup('pagerProtocol'), lookup('pagerEndpoint'), lookup('pagerFilterPolicy'))
}
```

The `SubscribeTopic` task can add an arbitrary number of subscriptions to an existing SNS topic. Each subscription is added by calling the `subscribe` method passing the subscription protocol, endpoint and optionally a filter policy as arguments. These arguments can be hard-coded or use lookups.

This task is safe to call multiple times as it will only add subscriptions which do not already exist.

#### `SetTopicAttributes`

The `seek.aws.sns.SetTopicAttributes` task can be used to set or update one or more attributes on an SNS topic.

**Example:**

```gradle
task enableAlertTopicErrorLogging(type: SetTopicAttributes) {
    def errorsRoleArn = lookup('errorsRoleArn')
    topicArn lookup('alertTopic')
    attribute('LambdaFailureFeedbackRoleArn', errorsRoleArn)
    attribute('SQSFailureFeedbackRoleArn', errorsRoleArn)
    attribute('ApplicationFailureFeedbackRoleArn', errorsRoleArn)
    attribute('HTTPFailureFeedbackRoleArn', errorsRoleArn)
}
```

The `SetTopicAttributes` task can set an arbitrary number of attributes on an existing SNS topic. Each attribute is set by calling the `attribute` method passing the attribute name and value as arguments. These arguments can be hard-coded or use lookups.

This task is safe to call multiple times as it will update the value of attributes that already exist.
