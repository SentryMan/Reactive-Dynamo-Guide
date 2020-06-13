package com.jojo.reactive.dynamo.config;

import java.net.ServerSocket;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
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

  private static DynamoDBProxyServer server;
  private static String port;
  private static final String TABLE_NAME = "some-table";
  private static final ProvisionedThroughput thouroghput = ProvisionedThroughput.builder()
      .readCapacityUnits((long) 100).writeCapacityUnits((long) 100).build();

  DynamoEmbeddedConfig() throws Exception {
    System.setProperty("sqlite4java.library.path", "native-libs");
    startEmbeddedDB();
  }

  // create embedded dynamo on random port
  private static void startEmbeddedDB() throws Exception {
    ServerSocket socket = new ServerSocket(0);

    port = Integer.toString(socket.getLocalPort());
    socket.close();
    server =
        ServerRunner.createServerFromCommandLineArgs(new String[] {"-inMemory", "-port", port});

    server.start();
  }

  // stop embedded dynamo
  public static void stopEmbeddedDB() throws Exception {
    server.stop();
  }

  @Bean
  public DynamoDbEnhancedAsyncClient dynamoDbAsyncClient() {

    DynamoDbAsyncClient dynamo = DynamoDbAsyncClient.builder()
        .endpointOverride(URI.create("http://localhost:" + port)).region(Region.US_EAST_1).build();
    createTable(dynamo);
    return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamo).build();
  }

  void createTable(DynamoDbAsyncClient dynamo) {

    CreateTableRequest request = CreateTableRequest.builder()
        .attributeDefinitions(
            AttributeDefinition.builder().attributeName("hashKey")
                .attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("sortKey")
                .attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("secondaryHashKey")
                .attributeType(ScalarAttributeType.N).build(),
            AttributeDefinition.builder().attributeName("secondarySortKey")
                .attributeType(ScalarAttributeType.N).build())
        .keySchema(
            KeySchemaElement.builder().attributeName("hashKey").keyType(KeyType.HASH).build(),
            KeySchemaElement.builder().attributeName("sortKey").keyType(KeyType.RANGE).build())
        .provisionedThroughput(thouroghput).globalSecondaryIndexes(secondaryIndex())
        .tableName(TABLE_NAME).build();

    Mono.fromFuture(dynamo.createTable(request)).subscribe();
  }

  GlobalSecondaryIndex secondaryIndex() {
    Projection projection = Projection.builder().projectionType("ALL").build();

    return GlobalSecondaryIndex.builder().indexName("index").keySchema(
        KeySchemaElement.builder().attributeName("secondaryHashKey").keyType(KeyType.HASH).build(),
        KeySchemaElement.builder().attributeName("secondarySortKey").keyType(KeyType.RANGE).build())
        .provisionedThroughput(thouroghput).projection(projection).build();
  }
}
