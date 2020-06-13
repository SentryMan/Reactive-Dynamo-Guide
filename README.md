
# Reactive-Dynamo-Guide
 So you want to use Reactive DynamoDB huh?
 This guide is made for you.
 
 It explains all the operations in the above repository.

## Dependencies

```xml
<properties>
	<java.version>1.8</java.version>
	<version.dynamodblocal>1.12.0</version.dynamodblocal>
	<version.sqllite4java>1.0.392</version.sqllite4java>
	<version.awssdk>2.13.18</version.awssdk>
</properties>
```

```xml
<dependency>
	<groupId>org.modelmapper</groupId>
	<artifactId>modelmapper</artifactId>
	<version>2.3.0</version>
</dependency>

<dependency>
	<groupId>software.amazon.awssdk</groupId>
	<artifactId>dynamodb-enhanced</artifactId>
	<version>${version.awssdk}</version>
</dependency>

<dependency>
	<groupId>software.amazon.awssdk</groupId>
	<artifactId>dynamodb</artifactId>
	<version>${version.awssdk}</version>
</dependency>

<dependency>
	<artifactId>DynamoDBLocal</artifactId>
	<groupId>com.amazonaws</groupId>
	<version>${version.dynamodblocal}</version>
</dependency>

<dependency>
	<artifactId>libsqlite4java-osx</artifactId>
	<groupId>com.almworks.sqlite4java</groupId>
	<type>dylib</type>
	<version>${version.sqllite4java}</version>
</dependency>
<dependency>
	<artifactId>sqlite4java</artifactId>
	<groupId>com.almworks.sqlite4java</groupId>
	<version>${version.sqllite4java}</version>
</dependency>
<dependency>
	<artifactId>libsqlite4java-linux-amd64</artifactId>
	<groupId>com.almworks.sqlite4java</groupId>
	<scope>test</scope>
	<type>so</type>
	<version>${version.sqllite4java}</version>
	</dependency>
<dependency>
	<artifactId>libsqlite4java-linux-i386</artifactId>
	<groupId>com.almworks.sqlite4java</groupId>
	<scope>test</scope>
	<type>so</type>
	<version>${version.sqllite4java}</version>
</dependency>
<dependency>
	<artifactId>sqlite4java-win32-x64</artifactId>
	<groupId>com.almworks.sqlite4java</groupId>
	<scope>test</scope>
	<type>dll</type>
	<version>${version.sqllite4java}</version>
</dependency>
<dependency>
	<artifactId>sqlite4java-win32-x86</artifactId>
	<groupId>com.almworks.sqlite4java</groupId>
	<scope>test</scope>
	<type>dll</type>
	<version>${version.sqllite4java}</version>
</dependency>
```

## Plugins
```xml
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-dependency-plugin</artifactId>
	<executions>
		<execution>
			<id>copy</id>
			<phase>compile</phase>
			<goals>
				<goal>copy-dependencies</goal>
			</goals>
			<configuration>							
				<includeScope>test</includeScope>
				<includeTypes>so,dll,dylib</includeTypes>
				<outputDirectory>${project.basedir}/native-libs</outputDirectory>
			</configuration>
		</execution>
	</executions>
</plugin>
```
## Setup

This section explains how to setup your initial classes

1. First you're going to instantiate a ``DynamoDbEnhancedAsyncClient``

```java
  @Bean
  public DynamoDbEnhancedAsyncClient getDynamoDbEnhancedAsyncClient() {

    return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
  }
  
  public DynamoDbAsyncClient getDynamoDbAsyncClient() {
    return DynamoDbAsyncClient.builder().endpointOverride(URI.create(dynamoDBEndpoint))
        .credentialsProvider(DefaultCredentialsProvider.builder().profileName(awsProfile).build())
        .build();
  }
``` 
2. Create your Model/DAO class 
Note: At the time of writing, the Async Dynamo Drivers only accept datatypes matching the Dynamo Scalar types,
so to store an object, you must serialize it to one of those types
```java
@DynamoDbBean
@Setter
public class EntityDAO {

  private String hashKey;
  private String sortKey;
  private int secondaryHashKey;
  private int secondarySortKey;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(value = "hashKey")
  public String gethashKey() {
    return hashKey;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(value = "sortKey")
  public String getSortKey() {
    return sortKey;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = {"index"})
  public int getSecondaryHashKey() {
    return secondaryHashKey;
  }

  @DynamoDbSecondarySortKey(indexNames = {"index"})
  public int getSecondarySortKey() {
    return secondarySortKey;
  }
}
``` 

3. Setup your repository class with a ``DynamoDbAsyncTable`` instance using the async client bean and the DAO class
```java

@Repository
public class RepositoryImpl implements TableRepository {

  private static final String TABLE_NAME = "some-table";
  private final DynamoDbAsyncTable<EntityDAO> dynamoDbAsyncTable;
  private final ModelMapper modelMapper;
  private final DynamoDbEnhancedAsyncClient asyncClient;

  public RepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbAsyncClient) {

    this.modelMapper = new ModelMapper();
    this.asyncClient = dynamoDbAsyncClient;
    this.dynamoDbAsyncTable =
        dynamoDbAsyncClient.table(TABLE_NAME, TableSchema.fromBean(EntityDAO.class));
  }
}
```
## Table Operations
This section explains how to do various operations on the table.

Note: Any method that immediately returns a ``Mono/Flux.fromFuture`` will execute the future before the mono is subscribed to. If you do not desire this behavior, you must defer execution to subscription time using ``Mono/Flux.defer``.
 
### Getting a single Item
The below method retrieves a single item from dynamo. The ``getItem`` method will extract the keys from the given entity and query the database for a matchching item.
 
```java
  public Mono<Entity> load(Entity entity) {
    return Mono.fromFuture(dynamoDbAsyncTable.getItem(modelMapper.map(entity, EntityDAO.class)))
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }
```

### Saving a single Item
The below method saves an entity to dynamo. The ``putItem`` method returns ``Void`` so we use the ``thenReturn`` operator is used to return the given entity when execution completes
 
```java
  public Mono<Entity> save(Entity entity) {
    return Mono.fromFuture(dynamoDbAsyncTable.putItem(modelMapper.map(entity, EntityDAO.class)))
        .thenReturn(entity)
        .doOnNext(dbResponse -> log.info("Successfully saved to Dynamo\n"));
  }
```
### Updating a single Item
The below method updates a single item in the table. Like the load method, it extracts the keys from the given object.

```java
  public Mono<Entity> update(Entity entity) {
    return Mono.fromFuture(dynamoDbAsyncTable.updateItem(modelMapper.map(entity, EntityDAO.class)))
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class))
        .doOnNext(dbResponse -> log.info("Successfully Updated Dynamo Record\n"));
  }
```

### Deleting a single Item
Deletes an item from the table. That's basically it.

```java
  public Mono<Entity> delete(Entity entity) {
    return Mono.fromFuture(dynamoDbAsyncTable.deleteItem(modelMapper.map(entity, EntityDAO.class)))
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }
```

### Batch Put

The below method batch saves a list of ``Entity`` into the db and returns the result.
Note: since the actual write request happens inside the ``Flux.flatmap`` the execution is deferred to subscription
```java
  public Mono<BatchWriteResult> batchPut(List<Entity> entityList) {
    return Flux.fromIterable(entityList)
        .map(entity -> modelMapper.map(entity, EntityDAO.class))
        //map each EntityDAO into a WriteBatch
        .map(
            dao ->
                WriteBatch.builder(EntityDAO.class)
                    .mappedTableResource(dynamoDbAsyncTable)
                    .addPutItem(dao)
                    .build())
        .collectList()
        //map List<WriteBatch> into BatchWriteItemEnhancedRequest
        .map(
            writeBatchList -> // Create a BatchWriteItemEnhancedRequest object
            BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build())
        .flatMap(
            batchWriteItemEnhancedRequest ->
                Mono.fromFuture(asyncClient.batchWriteItem(batchWriteItemEnhancedRequest)));
  }
```
### Batch Delete

The below method batch deletes items by converting a list of ``Entity`` into a ``BatchWriteItemEnhancedRequest`` and returns the result.

```java
  public Mono<BatchWriteResult> batchPut(List<Entity> entityList) {
    return Flux.fromIterable(entityList)
        .map(entity -> modelMapper.map(entity, EntityDAO.class))
        //map each EntityDAO into a WriteBatch
        .map(
            dao ->
                WriteBatch.builder(EntityDAO.class)
                    .mappedTableResource(dynamoDbAsyncTable)
                    .addDeleteItem(dao)
                    .build())
        .collectList()
        //map List<WriteBatch> into BatchWriteItemEnhancedRequest
        .map(
            writeBatchList -> // Create a BatchWriteItemEnhancedRequest object
            BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build())
        .flatMap(
            batchWriteItemEnhancedRequest ->
                Mono.fromFuture(asyncClient.batchWriteItem(batchWriteItemEnhancedRequest)));
  }
```

### Querying the database

The Async SDK provides a ``QueryConditional`` Object with which you must make your queries.
In the below method, I query for items matching ``hashkey = :hashvalue and sortkey = :sortvalue`` and get the response as a ``Flux``.

```java
 @Override
  public Flux<Entity> query(String hashKey, String sortKey) {
    QueryConditional queryCondition =
        QueryConditional.keyEqualTo(
            key -> key.partitionValue(hashKey).sortValue(sortKey).build());
    return Flux.from(dynamoDbAsyncTable.query(queryCondition).items())
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }
```

### Querying a secondary Index

In the below method, I query the secondary index for items matching ``hashkey = :hashvalue and sortkey >= :sortvalue`` and get the response as a ``Flux``.
The extra ``flatmap`` is because ``DynamoDbAsyncIndex.query`` returns a publisher of pages.
```java
 @Override
  public Flux<Entity> queryIndex(int hashKey, int sortKey, String indexName) {
    // define what index to query
    DynamoDbAsyncIndex<EntityDAO> asyncIndex = dynamoDbAsyncTable.index(indexName);

    // Create a QueryConditional object that's used in the query operation
    QueryConditional queryCondition =
        QueryConditional.sortGreaterThanOrEqualTo(
            key -> key.partitionValue(hashKey).sortValue(sortKey).build());

    return Flux.from(asyncIndex.query(queryCondition))
        // convert page contents into a flux
        .flatMapIterable(page -> page.items())
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }
```
# EmbeddedTesting

There is no ``EmbeddedDynamo`` class in the new sdk, so to create an in-memory database you must run a ``DynamoDBProxyServer`` then create the embedded table using the ``DynamoDbAsyncClient``

```java
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

```
