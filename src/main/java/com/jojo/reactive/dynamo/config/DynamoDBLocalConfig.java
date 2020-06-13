package com.jojo.reactive.dynamo.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Profile("!test")
@Configuration
public class DynamoDBLocalConfig {

  @Value("${amazon.profile}")
  private String awsProfile;

  @Value("${amazon.dynamodb.endpoint}")
  private String dynamoDBEndpoint;


  @Bean
  public DynamoDbEnhancedAsyncClient getDynamoDbEnhancedAsyncClient() {

    return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
  }

  public DynamoDbAsyncClient getDynamoDbAsyncClient() {
    return DynamoDbAsyncClient.builder().endpointOverride(URI.create(dynamoDBEndpoint))
        .credentialsProvider(DefaultCredentialsProvider.builder().profileName(awsProfile).build())
        .region(Region.US_EAST_1).build();
  }
}
