package com.jojo.reactive.dynamo.repository;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
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
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.Builder;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

@Slf4j
@Repository
public class RepositoryImpl implements TableRepository {

  private static final String TABLE_NAME = "some-table";
  private final DynamoDbAsyncTable<EntityDAO> asyncTable;
  private final DynamoDbEnhancedAsyncClient asyncClient;
  private final TypeMap<EntityDAO, Entity> modelMapper;
  private final TypeMap<Entity, EntityDAO> daoMapper;

  @Autowired
  public RepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbAsyncClient) {

    final ModelMapper mapper = new ModelMapper();
    asyncClient = dynamoDbAsyncClient;
    asyncTable = dynamoDbAsyncClient.table(TABLE_NAME, TableSchema.fromBean(EntityDAO.class));

    modelMapper = mapper.createTypeMap(EntityDAO.class, Entity.class);
    daoMapper = mapper.createTypeMap(Entity.class, EntityDAO.class);
  }

  // Load Method
  @Override
  public Mono<Entity> load(Entity entity) {

    return Mono.just(entity)
        .map(daoMapper::map)
        .map(asyncTable::getItem)
        // convert getItem future to mono
        .flatMap(Mono::fromFuture)
        .map(modelMapper::map);
  }

  // Save and Update Methods
  @Override
  public Mono<Entity> save(Entity entity) {

    return Mono.just(entity)
        .map(daoMapper::map)
        .map(asyncTable::putItem)
        // convert save future to mono
        .flatMap(Mono::fromFuture)
        .thenReturn(entity)
        .doOnNext(dbResponse -> log.info("Successfully saved to Dynamo\n"));
  }

  @Override
  public Mono<Entity> update(Entity entity) {

    return Mono.just(entity)
        .map(daoMapper::map)
        .map(asyncTable::updateItem)
        .flatMap(Mono::fromFuture)
        .map(modelMapper::map)
        .doOnNext(dbResponse -> log.info("Successfully Updated Dynamo Record\n"));
  }

  // Delete
  @Override
  public Mono<Entity> delete(Entity entity) {

    return Mono.just(entity)
        .map(daoMapper::map)
        .map(asyncTable::deleteItem)
        .flatMap(Mono::fromFuture)
        .map(modelMapper::map);
  }

  // advanced operations
  @Override
  public Flux<Entity> query(String hashKey, String sortKey) {

    // Create a QueryConditional object that's used in the query operation
    final QueryConditional queryCondition =
        QueryConditional.keyEqualTo(key -> key.partitionValue(hashKey).sortValue(sortKey).build());

    // convert sdkPublisher to Reactor publisher
    return Flux.from(asyncTable.query(queryCondition).items()).map(modelMapper::map);
  }

  @Override
  public Flux<Entity> queryIndex(int indexHashKey, int indexSortKey, String indexName) {
    // define what index to query
    final DynamoDbAsyncIndex<EntityDAO> asyncIndex = asyncTable.index(indexName);

    // Create a QueryConditional object that's used in the query operation
    final QueryConditional queryCondition =
        QueryConditional.sortGreaterThanOrEqualTo(
            key -> key.partitionValue(indexHashKey).sortValue(indexSortKey).build());

    return Flux.from(asyncIndex.query(queryCondition))
        // convert page contents into a flux
        .flatMapIterable(Page::items)
        .map(modelMapper::map);
  }

  // batch operations
  @Override
  public Flux<EntityDAO> batchPut(List<Entity> entityList) {

    return Flux.fromIterable(entityList)
        .map(daoMapper::map)
        .map(
            dao ->
                WriteBatch.builder(EntityDAO.class)
                    .mappedTableResource(asyncTable)
                    .addPutItem(dao)
                    .build())
        .buffer(25)
        // Create a BatchWriteItemEnhancedRequest object
        .map(BatchWriteItemEnhancedRequest.builder()::writeBatches)
        .map(Builder::build)
        .map(asyncClient::batchWriteItem)
        .flatMap(Mono::fromFuture)
        // if all items in the batch saved, the Flux will complete without
        // emitting data
        .flatMapIterable(result -> result.unprocessedPutItemsForTable(asyncTable));
  }

  @Override
  public Flux<Key> batchDelete(List<Entity> entityList) {
    return Flux.fromIterable(entityList)
        .map(daoMapper::map)
        .map(
            dao ->
                WriteBatch.builder(EntityDAO.class)
                    .mappedTableResource(asyncTable)
                    .addDeleteItem(dao)
                    .build())
        .buffer(25)
        // Create a BatchWriteItemEnhancedRequest object
        .map(BatchWriteItemEnhancedRequest.builder()::writeBatches)
        .map(Builder::build)
        .map(asyncClient::batchWriteItem)
        .flatMap(Mono::fromFuture)
        // if all items in the batch deleted, the Flux will complete without emitting data
        .flatMapIterable(result -> result.unprocessedDeleteItemsForTable(asyncTable));
  }
}
