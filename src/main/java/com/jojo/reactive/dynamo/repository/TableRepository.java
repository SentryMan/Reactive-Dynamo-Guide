package com.jojo.reactive.dynamo.repository;

import java.util.List;

import com.jojo.reactive.dynamo.models.Entity;
import com.jojo.reactive.dynamo.models.EntityDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public interface TableRepository {

  Mono<Entity> save(Entity cancelNotification);

  Mono<Entity> load(Entity notificationModel);

  Mono<Entity> update(Entity autopayEvent);

  Mono<Entity> delete(Entity databaseEntry);

  Flux<Entity> query(String hashKey, String sortKey);

  Flux<Entity> queryIndex(int i, int j, String indexName);

  Flux<EntityDAO> batchPut(List<Entity> databaseEntry);

  Flux<Key> batchDelete(List<Entity> entityList);
}
