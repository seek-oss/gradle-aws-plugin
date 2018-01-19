# Gradle AWS Plugin Example Project

This Gradle project provides an example of a simple service that is deployed via CloudFormation.

This service consists of two S3 buckets and a Lambda function. When an object is put to the source bucket a notification is sent to the Lambda function which then copies the object to the destination bucket.

This project provides a full example of setting up environment specific config, ordering deployment tasks, and deployment using the CloudFormation plugin.
