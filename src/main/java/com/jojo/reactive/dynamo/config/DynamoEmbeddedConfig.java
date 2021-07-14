package com.jojo.reactive.dynamo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Profile("test")
@Configuration
public class DynamoEmbeddedConfig {

  private static final String TABLE_NAME = "some-table";
  private static final ProvisionedThroughput thouroghput =
      ProvisionedThroughput.builder()
          .readCapacityUnits((long) 100)
          .writeCapacityUnits((long) 100)
          .build();

  DynamoEmbeddedConfig() {
    System.setProperty("sqlite4java.library.path", "native-libs");
  }

  @Bean
  public DynamoDbEnhancedAsyncClient dynamoDbAsyncClient() {

    final DynamoDbAsyncClient dynamo = DynamoDBEmbedded.create().dynamoDbAsyncClient();
    createTable(dynamo);
    return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamo).build();
  }

  void createTable(DynamoDbAsyncClient dynamo) {

    final CreateTableRequest request =
        CreateTableRequest.builder()
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("hashKey")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("sortKey")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("secondaryHashKey")
                    .attributeType(ScalarAttributeType.N)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("secondarySortKey")
                    .attributeType(ScalarAttributeType.N)
                    .build())
            .keySchema(
                KeySchemaElement.builder().attributeName("hashKey").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("sortKey").keyType(KeyType.RANGE).build())
            .provisionedThroughput(thouroghput)
            .globalSecondaryIndexes(secondaryIndex())
            .tableName(TABLE_NAME)
            .build();

    Mono.fromFuture(dynamo.createTable(request)).subscribe();
  }

  GlobalSecondaryIndex secondaryIndex() {
    final Projection projection = Projection.builder().projectionType("ALL").build();

    return GlobalSecondaryIndex.builder()
        .indexName("index")
        .keySchema(
            KeySchemaElement.builder()
                .attributeName("secondaryHashKey")
                .keyType(KeyType.HASH)
                .build(),
            KeySchemaElement.builder()
                .attributeName("secondarySortKey")
                .keyType(KeyType.RANGE)
                .build())
        .provisionedThroughput(thouroghput)
        .projection(projection)
        .build();
  }
}
