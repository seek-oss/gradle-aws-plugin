# Gradle AWS Plugin Example Project

This Gradle project provides an example of a simple service that is deployed via CloudFormation. The service consists of two S3 buckets and a Lambda function. When an object is put to the source bucket a notification is sent to the Lambda function which then copies the object to the destination bucket.

This project provides a full example of setting up environment specific config, ordering of tasks, and deployment using the CloudFormation plugin.

Deployment of the project requires that the environment be specified. The environment can equal either "development" or "production". If the environment is not specified on the command line the plugin will attempt to determine the environment by querying SSM Parameter Store.

The following command will deploy the service by specifying the environment on the command line:

```
gradle deploy -Penvironment=development
```

The environment name is used to determine which configuration files to load. Since the project makes use of S3 buckets (which must have globally unique names) you'll need to rename the bucket names specified in [config/development.conf](config/development.conf) and [config/production.conf](config/production.conf).
