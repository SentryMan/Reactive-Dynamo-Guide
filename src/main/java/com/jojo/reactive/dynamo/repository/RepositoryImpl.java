package com.jojo.reactive.dynamo.repository;

import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.jojo.reactive.dynamo.models.Entity;
import com.jojo.reactive.dynamo.models.EntityDAO;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

@Slf4j
@Repository
public class RepositoryImpl implements TableRepository {

  private static final String TABLE_NAME = "some-table";
  private final DynamoDbAsyncTable<EntityDAO> dynamoDbAsyncTable;
  private final ModelMapper modelMapper;
  private final DynamoDbEnhancedAsyncClient asyncClient;

  // When a future is converted into a mono, the future runs immediately
  // unless deferred to subscription time
  // like so: Flux/Mono.defer(()->load(entity))

  @Autowired
  public RepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbAsyncClient) {

    this.modelMapper = new ModelMapper();
    this.asyncClient = dynamoDbAsyncClient;
    this.dynamoDbAsyncTable =
        dynamoDbAsyncClient.table(TABLE_NAME, TableSchema.fromBean(EntityDAO.class));
  }

  // Load Method
  @Override
  public Mono<Entity> load(Entity entity) {

    // convert getItem future to mono
    return Mono.fromFuture(dynamoDbAsyncTable.getItem(modelMapper.map(entity, EntityDAO.class)))
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }

  // Save and Update Methods

  @Override
  public Mono<Entity> save(Entity entity) {
    // convert save future to mono
    return Mono.fromFuture(dynamoDbAsyncTable.putItem(modelMapper.map(entity, EntityDAO.class)))
        // the completable future is void so we return the given entity
        .thenReturn(entity).doOnNext(dbResponse -> log.info("Successfully saved to Dynamo\n"));
  }

  @Override
  public Mono<Entity> update(Entity entity) {

    return Mono.fromFuture(dynamoDbAsyncTable.updateItem(modelMapper.map(entity, EntityDAO.class)))
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class))
        .doOnNext(dbResponse -> log.info("Successfully Updated Dynamo Record\n"));
  }

  // Delete
  public Mono<Entity> delete(Entity entity) {

    return Mono.fromFuture(dynamoDbAsyncTable.deleteItem(modelMapper.map(entity, EntityDAO.class)))
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }

  // advanced operations
  @Override
  public Flux<Entity> query(String hashKey, String sortKey) {

    // Create a QueryConditional object that's used in the query operation
    QueryConditional queryCondition =
        QueryConditional.keyEqualTo(key -> key.partitionValue(hashKey).sortValue(sortKey).build());

    return Flux.from(dynamoDbAsyncTable.query(queryCondition).items())
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }

  @Override
  public Flux<Entity> queryIndex(int hashKey, int sortKey, String indexName) {
    // define what index to query
    DynamoDbAsyncIndex<EntityDAO> asyncIndex = dynamoDbAsyncTable.index(indexName);

    // Create a QueryConditional object that's used in the query operation
    QueryConditional queryCondition = QueryConditional
        .sortGreaterThanOrEqualTo(key -> key.partitionValue(hashKey).sortValue(sortKey).build());

    return Flux.from(asyncIndex.query(queryCondition))
        // convert page contents into a flux
        .flatMapIterable(page -> page.items())
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }

  // batch
  @Override
  public Flux<EntityDAO> batchPut(List<Entity> entityList) {

    return Flux.fromIterable(entityList).map(entity -> modelMapper.map(entity, EntityDAO.class))
        .map(dao -> WriteBatch.builder(EntityDAO.class).mappedTableResource(dynamoDbAsyncTable)
            .addPutItem(dao).build())
        .buffer(25).map(writeBatchList -> // Create a BatchWriteItemEnhancedRequest object
        BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build())
        .flatMap(batchWriteItemEnhancedRequest -> Mono
            .fromFuture(asyncClient.batchWriteItem(batchWriteItemEnhancedRequest)))
        .flatMapIterable(result -> result.unprocessedPutItemsForTable(dynamoDbAsyncTable))
        .doOnComplete(() -> System.out.println("Completed BatchPut"));
  }

  @Override
  public Flux<Key> batchDelete(List<Entity> entityList) {

    return Flux.fromIterable(entityList).map(entity -> modelMapper.map(entity, EntityDAO.class))
        .map(dao -> WriteBatch.builder(EntityDAO.class).mappedTableResource(dynamoDbAsyncTable)
            .addDeleteItem(dao).build())
        .buffer(25).map(writeBatchList -> // Create a BatchWriteItemEnhancedRequest object
        BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build())
        .flatMap(batchWriteItemEnhancedRequest -> Mono
            .fromFuture(asyncClient.batchWriteItem(batchWriteItemEnhancedRequest)))
        .flatMapIterable(result -> result.unprocessedDeleteItemsForTable(dynamoDbAsyncTable))
        .doOnComplete(() -> System.out.println("Completed BatchDelete"));
  }
}
