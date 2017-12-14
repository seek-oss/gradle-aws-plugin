# Gradle AWS Plugin
This Gradle plugin provides a means of orchestrating the provisioning of AWS infrastructure and managing configuration parameters across environments.

## Getting Started

Apply the AWS plugin as described here. TODO: plugins link.

If you want to use CloudFormation also apply the CloudFormation plugin as described here. TODO: plugins link.

## The Plugins
This repository is an umbrella for three logically separate but related Gradle plugins: `seek.aws`, `seek.config`, and `seek.cloudformation`.

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

The `region` property is used to determine the [AWS region](http://docs.aws.amazon.com/general/latest/gr/rande.html) that tasks will operate against by default. All AWS tasks also have an optional `region` property which takes precedence over the region specified in the `aws` extension. Tasks that do not specify a `region` use the region defined in the `aws` extension. The `region` property is not technically required in `aws` but if it is not specified every AWS task must specify the property.

All AWS tasks use the [default credentials provider chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html). The `profile` property of `aws` can be used if you use named profiles in `~/.aws/credentials` otherwise it does not need to be specified.

The properties of `aws` can use `lookup` methods to lazily set values via configuration. Lookups are described in the next section.

### Config Plugin
The Config plugin is applied by the top level AWS plugin so it does need to be manually applied to a Gradle project. This plugin provides a means of specifying the location of configuration parameters and then lazily obtaining their values when tasks are run.

The Config plugin is configured via a plugin extension named `config`:

```
config {
    files fileTree('config').include('*.conf')
}

```
The methods of the `config` extension are described below:

|Method                 |Argument type   |Description                               |Required|Default
|-----------------------|----------------|------------------------------------------|--------|-------
|`naming`                |`String`        |Config file naming convention             |No      |environment
|`files`                |`FileCollection`|Full set of configuration files           |No      |-
|`addFiles`             |`FileCollection`|Adds configuration files                  |No      |-
|`allowProjectOverrides`|`Boolean`       |Whether project properties override config|No      |`true`
|`allowCommonConfig`    |`Boolean`       |Whether common config is allows           |No      |`true`
|`commonConfigName`     |`String`        |Name of the common configuration file     |No      |"common"

The `naming` property specifies how configuration files are named. The default is "environment". This means that at runtime the value of the `environment` Gradle property will determine the name of the configuration file(s) to load. If `environment=production` the Config plugin will load any files named `production.conf`, `production.json`, and `production.properties`.

The `naming` property does not have to be one dimensional. For example, consider a service that is deployed to multiple regions within an environment. A possible naming scheme is "environment.region". At runtime the values of the properties `environment` and `region` will be resolved to determine the name of the config files. If `environment=production` and `region=ap-southeast-2` then the plugin will look for config files named `production.ap-southeast-2.(conf|json|properties)`.

The `files` property sets the `FileCollection` that contains all configuration files to be used. `addFiles` can be used to additively accumulate files. For example, in a multi-project Gradle build the root project might set top level common configuration files and then subprojects might add their specific configuration files using `addFiles`.

The `allowProjectOverrides` property specifies whether Gradle project properties should be considered before looking up configuration files. This is useful for overriding configuration values on the command line. For example, if `allowProjectOverrides` is true (the default) then:

```
gradle createOrUpdateStack -PbucketName=my-bucket
```

would use the value of `bucketName` specified on the command line rather than the value of `bucketName` found in configuration.

The `allowCommonConfig` property specifies whether common configuration files are allowed. An application that is deployed to multiple environments will often still have a core set of configuration parameters that are common to all environments. When this property is true (the default) the plugin will load any configuration files that match `commonConfigName` and attempt to resolve parameters there if they can not be resolved in environment specific files.

#### Resolution of Configuration Values
The Config plugin uses the [Lightbend config library](https://github.com/lightbend/config) (formerly Typesafe config) to parse configuration files.


### CloudFormation Plugin

## Examples
