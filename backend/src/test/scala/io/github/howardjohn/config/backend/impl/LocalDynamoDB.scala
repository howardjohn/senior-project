package io.github.howardjohn.config.backend.impl

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2._

object LocalDynamoDB {
  def client(): AmazonDynamoDBAsync =
    AmazonDynamoDBAsyncClient
      .asyncBuilder()
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "credentials")))
      .withEndpointConfiguration(new EndpointConfiguration("http://localhost:8042", ""))
      .build()
}
