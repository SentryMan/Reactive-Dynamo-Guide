package com.jojo.reactive.dynamo.repository;

import java.util.List;
import com.jojo.reactive.dynamo.models.Entity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;

public interface TableRepository {

  Mono<Entity> save(Entity cancelNotification);

  Mono<Entity> load(Entity notificationModel);

  Mono<Entity> update(Entity autopayEvent);

  Mono<Entity> delete(Entity databaseEntry);

  Flux<Entity> query(String hashKey, String sortKey);

  Flux<Entity> queryIndex(int i, int j, String indexName);

  Mono<BatchWriteResult> batchPut(List<Entity> databaseEntry);

  Mono<BatchWriteResult> batchDelete(List<Entity> entityList);
}
