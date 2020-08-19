
# Reactive-Dynamo-Guide
 So you want to use Reactive DynamoDB huh?
 This guide is made for you.
 
 It explains all the operations in the above repository.
 This guide assumes that you're at least somewhat familiar with reactive programming and reactive types. It also assumes you have set up the basic AWS CLI.

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
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <version>2.3.1.RELEASE</version>
</dependency>

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

This section explains how to setup your initial classes for a local enviroment.

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
 
**Note:** At the time of writing, the Async Dynamo Drivers only accept datatypes matching the Dynamo Scalar types,
so to store an object, you must serialize it to one of those types. I find using ``ObjectMapper`` to serialize to a string to be a relatively easy workaround.
```java
@DynamoDbBean
@Setter
public class EntityDAO {

  private String hashKey;
  private String sortKey;
  private int indexHashKey;
  private int indexSortKey;

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
// for secondary indexes you must state the index names
  @DynamoDbSecondaryPartitionKey(indexNames = {"index"})
  public int getIndexHashKey() {
    return indexHashKey;
  }

  @DynamoDbSecondarySortKey(indexNames = {"index"})
  public int getIndexSortKey() {
    return indexSortKey;
  }
}
``` 

3. Setup your repository class with a ``DynamoDbAsyncTable`` instance using the async client bean and the DAO class. (``DynamoDbAsyncTable`` is the async equivalent to the old ``DynamoDBMapper``.)
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

Most operations of the Enhanced Dynamo SDK return a ``CompletableFuture``, so to make these operations non-blocking and reactive, you can use the ``fromFuture()`` method of a Reactive Publisher to convert the future into a reactive stream.

**Note:** Futures passed to a ``Mono/Flux.fromFuture`` will execute the future before the Publisher is subscribed to.  e.g:
```java

CompletableFuture<DAO> future = dynamoDbAsyncTable.updateItem(entityDAO)
//without deferring, the save method executes before the flux emits any data
  
  Flux.just(1,2,3).then(Mono.fromFuture(future).subscribe();
```
Be sure you are running the future inside the reactive stream.
 
 
### Getting a single Item
The below method retrieves a single item from dynamo. The ``getItem`` method will extract the keys from the given entity and query the database for one matching item.
 
```java
  public Mono<Entity> load(Entity entity) {

    return Mono.just(entity)
        .map(e -> modelMapper.map(entity, EntityDAO.class))
        //run the future in the reactive stream
        .map(asyncTable::getItem)
        // convert getItem future to a mono
        .flatMap(Mono::fromFuture)
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }
```

### Saving a single Item
The below method saves an entity to dynamo. The ``putItem`` method returns ``CompletableFuture<Void>`` so we use the ``thenReturn`` operator on the stream to return the given entity when execution completes.
 
```java
  public Mono<Entity> save(Entity entity) {

    return Mono.just(entity)
        .map(e -> modelMapper.map(entity, EntityDAO.class))
        .map(asyncTable::putItem)
        // convert save future to mono
        .flatMap(Mono::fromFuture)
        .thenReturn(entity)
        .doOnNext(dbResponse -> log.info("Successfully saved to Dynamo\n"));
  }
```
### Updating a single Item
The below method updates a single item in the table. Like the load method, it extracts the keys from the given object.

```java
  public Mono<Entity> update(Entity entity) {
    return Mono.just(entity)
        .map(e -> modelMapper.map(entity, EntityDAO.class))
        .map(asyncTable::updateItem)
        .flatMap(Mono::fromFuture)
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class))
        .doOnNext(dbResponse -> log.info("Successfully Updated Dynamo Record\n"));
  }
```

### Deleting a single Item
Deletes an item from the table. That's basically it.

```java
  public Mono<Entity> delete(Entity entity) {
    return Mono.just(entity)
        .map(e -> modelMapper.map(entity, EntityDAO.class))
        .map(asyncTable::deleteItem)
        .flatMap(Mono::fromFuture)
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }
```
### Querying the database
The query method of the sdk returns a reactive ``PagePublisher<T>`` which is easily converted into a Reactor publisher using the ``Flux.from`` method.

To query the database, the SDK has provided a [``QueryConditional``](https://sdk.amazonaws.com/java/api/latest/index.html?software/amazon/awssdk/enhanced/dynamodb/model/QueryConditional.html) with which you must make your queries. It has various built-in methods for various queries.

In the below method, I use the ``keyEqualTo`` method and pass the key values to build a query condition equivalent to  ``hashkey = :hashvalue and sortkey = :sortvalue``. I use the ``.items()`` method on the query to receive the stream of desired items.

```java
  public Flux<Entity> query(String hashKey, String sortKey) {
    QueryConditional queryCondition =
        QueryConditional.keyEqualTo(
            key -> key.partitionValue(hashKey).sortValue(sortKey).build());
            
    //convert sdkPublisher to Reactor Publisher
    return Flux.from(dynamoDbAsyncTable.query(queryCondition).items())
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }
```
### Querying a secondary Index

The index query methods of the sdk return a reactive ``SdkPublisher<T>``. which requires an extra step to process.

In the below method, I query the secondary index for items matching ``indexHashkey = :indexHashvalue and indexSortkey >= :indexSortValue`` and get the response as a ``Flux<Page>``. I then use the ``flatmapIterable`` to extract the items from the ``Page``.
```java
  public Flux<Entity> queryIndex(int indexHashKey, int indexSortKey, String indexName) {
    // define what index to query
    DynamoDbAsyncIndex<EntityDAO> asyncIndex = dynamoDbAsyncTable.index(indexName);

    // Create a QueryConditional object that's used in the query operation
    QueryConditional queryCondition =
        QueryConditional.sortGreaterThanOrEqualTo(
            key -> key.partitionValue(indexHashKey).sortValue(indexSortKey).build());

    return Flux.from(asyncIndex.query(queryCondition))
        // convert page contents into a flux
        .flatMapIterable(page -> page.items())
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }
```
### Batch Put

The below method batch saves a list of ``Entity`` into the db and completes the ``Flux``.
If any part of the batch fails to process, the returned flux will emit the unprocessed entities.

**Note:** As you know, the limit of items in a single write batch is 25. To bypass this, I've written the following batch methods to automatically partition the given list using ``buffer`` and make multiple consecutive write batch requests if the given list has more than 25 elements.
 
```java
  public Flux<EntityDAO> batchPut(List<Entity> entityList) {

    return Flux.fromIterable(entityList)
        .map(entity -> modelMapper.map(entity, EntityDAO.class))
        .map(
            dao ->
                WriteBatch.builder(EntityDAO.class)
                    .mappedTableResource(dynamoDbAsyncTable)
                    .addPutItem(dao)
                    .build())
        .buffer(25)
        .map(
            writeBatchList -> // Create a BatchWriteItemEnhancedRequest object
            BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build())
        .map(asyncClient::batchWriteItem)
        .flatMap(Mono::fromFuture)
        // if all items in the batch saved, the Flux will complete without emitting data
        .flatMapIterable(result -> result.unprocessedPutItemsForTable(dynamoDbAsyncTable));
  }
 ```
### Batch Delete

The below method batch deletes items by converting a list of ``Entity`` into a ``BatchWriteItemEnhancedRequest``.
If any request fails to process, the returned flux will emit the keys of the unprocessed data.
```java
  public Flux<Key> batchDelete(List<Entity> entityList) {

    return Flux.fromIterable(entityList)
        .map(entity -> modelMapper.map(entity, EntityDAO.class))
        .map(
            dao ->
                WriteBatch.builder(EntityDAO.class)
                    .mappedTableResource(dynamoDbAsyncTable)
                    .addDeleteItem(dao)
                    .build())
        .buffer(25)
        .map(
            writeBatchList -> // Create a BatchWriteItemEnhancedRequest object
            BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build())
        .map(asyncClient::batchWriteItem)
        .flatMap(Mono::fromFuture)        
        // if all items in the batch were processed, the Flux will complete without emitting data
        .flatMapIterable(result -> result.unprocessedDeleteItemsForTable(dynamoDbAsyncTable));
  }
```

# Embedded Testing

There is no ``EmbeddedDynamo`` class in the new sdk, so to create an in-memory database you must run a ``DynamoDBProxyServer`` then create the in-memory table using the ``DynamoDbAsyncClient``.

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
        .endpointOverride(URI.create("http://localhost:" + port)).build();
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
            AttributeDefinition.builder().attributeName("indexHashKey")
                .attributeType(ScalarAttributeType.N).build(),
            AttributeDefinition.builder().attributeName("indexSortKey")
                .attributeType(ScalarAttributeType.N).build())
        .keySchema(
            KeySchemaElement.builder().attributeName("hashKey").keyType(KeyType.HASH).build(),
            KeySchemaElement.builder().attributeName("sortKey").keyType(KeyType.RANGE).build())
        .provisionedThroughput(thouroghput).globalSecondaryIndexes(secondaryIndex())
        .tableName(TABLE_NAME).build();

    Mono.fromFuture(dynamo.createTable(request)).block();
  }

  GlobalSecondaryIndex secondaryIndex() {
    Projection projection = Projection.builder().projectionType("ALL").build();

    return GlobalSecondaryIndex.builder().indexName("index").keySchema(
        KeySchemaElement.builder().attributeName("indexHashKey").keyType(KeyType.HASH).build(),
        KeySchemaElement.builder().attributeName("indexSortKey").keyType(KeyType.RANGE).build())
        .provisionedThroughput(thouroghput).projection(projection).build();
  }
}

```
