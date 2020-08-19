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
  private final DynamoDbAsyncTable<EntityDAO> asyncTable;
  private final ModelMapper modelMapper;
  private final DynamoDbEnhancedAsyncClient asyncClient;

  @Autowired
  public RepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbAsyncClient) {

    this.modelMapper = new ModelMapper();
    this.asyncClient = dynamoDbAsyncClient;
    this.asyncTable = dynamoDbAsyncClient.table(TABLE_NAME, TableSchema.fromBean(EntityDAO.class));
  }

  // Load Method
  @Override
  public Mono<Entity> load(Entity entity) {

    return Mono.just(entity)
        .map(e -> modelMapper.map(entity, EntityDAO.class))
        .map(asyncTable::getItem)
        // convert getItem future to mono
        .flatMap(Mono::fromFuture)
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }

  // Save and Update Methods
  @Override
  public Mono<Entity> save(Entity entity) {

    return Mono.just(entity)
        .map(e -> modelMapper.map(entity, EntityDAO.class))
        .map(asyncTable::putItem)
        // convert save future to mono
        .flatMap(Mono::fromFuture)
        .thenReturn(entity)
        .doOnNext(dbResponse -> log.info("Successfully saved to Dynamo\n"));
  }

  @Override
  public Mono<Entity> update(Entity entity) {

    return Mono.just(entity)
        .map(e -> modelMapper.map(entity, EntityDAO.class))
        .map(asyncTable::updateItem)
        .flatMap(Mono::fromFuture)
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class))
        .doOnNext(dbResponse -> log.info("Successfully Updated Dynamo Record\n"));
  }

  // Delete
  public Mono<Entity> delete(Entity entity) {

    return Mono.just(entity)
        .map(e -> modelMapper.map(entity, EntityDAO.class))
        .map(asyncTable::deleteItem)
        .flatMap(Mono::fromFuture)
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }

  // advanced operations
  @Override
  public Flux<Entity> query(String hashKey, String sortKey) {

    // Create a QueryConditional object that's used in the query operation
    QueryConditional queryCondition =
        QueryConditional.keyEqualTo(key -> key.partitionValue(hashKey).sortValue(sortKey).build());

    // convert sdkPublisher to Reactor publisher
    return Flux.from(asyncTable.query(queryCondition).items())
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }

  @Override
  public Flux<Entity> queryIndex(int indexHashKey, int indexSortKey, String indexName) {
    // define what index to query
    DynamoDbAsyncIndex<EntityDAO> asyncIndex = asyncTable.index(indexName);

    // Create a QueryConditional object that's used in the query operation
    QueryConditional queryCondition =
        QueryConditional.sortGreaterThanOrEqualTo(
            key -> key.partitionValue(indexHashKey).sortValue(indexSortKey).build());

    return Flux.from(asyncIndex.query(queryCondition))
        // convert page contents into a flux
        .flatMapIterable(Page::items)
        .map(dbResponse -> modelMapper.map(dbResponse, Entity.class));
  }

  // batch operations
  @Override
  public Flux<EntityDAO> batchPut(List<Entity> entityList) {

    return Flux.fromIterable(entityList)
        .map(entity -> modelMapper.map(entity, EntityDAO.class))
        .map(
            dao ->
                WriteBatch.builder(EntityDAO.class)
                    .mappedTableResource(asyncTable)
                    .addPutItem(dao)
                    .build())
        .buffer(25)
        // Create a BatchWriteItemEnhancedRequest object
        .map(
            writeBatchList ->
                BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build())
        .map(asyncClient::batchWriteItem)
        .flatMap(Mono::fromFuture)
        // if all items in the batch saved, the Flux will complete without
        // emitting data
        .flatMapIterable(result -> result.unprocessedPutItemsForTable(asyncTable));
  }

  @Override
  public Flux<Key> batchDelete(List<Entity> entityList) {

    return Flux.fromIterable(entityList)
        .map(entity -> modelMapper.map(entity, EntityDAO.class))
        .map(
            dao ->
                WriteBatch.builder(EntityDAO.class)
                    .mappedTableResource(asyncTable)
                    .addDeleteItem(dao)
                    .build())
        .buffer(25)
        // Create a BatchWriteItemEnhancedRequest object
        .map(
            writeBatchList ->
                BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatchList).build())
        .map(asyncClient::batchWriteItem)
        .flatMap(Mono::fromFuture)
        // if all items in the batch deleted, the Flux will complete without emitting data
        .flatMapIterable(result -> result.unprocessedDeleteItemsForTable(asyncTable));
  }
}
