package com.jojo.reactive.dynamo.repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jojo.reactive.dynamo.models.Entity;

import reactor.test.StepVerifier;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class TableRepositoryTest {

  @Autowired TableRepository repo;
  Entity entity = new Entity();

  @BeforeEach
  void setUp() throws Exception {

    repo.save(entity).delayElement(Duration.ofMillis(2000)).block();
  }

  @Test
  final void testLoad() {

    StepVerifier.create(repo.load(new Entity()).delayElement(Duration.ofMillis(3000)))
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  final void testDelete() {
    StepVerifier.create(repo.delete(new Entity()).delayElement(Duration.ofMillis(3000)))
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  final void testQuery() {
    final List<Entity> list = new ArrayList<Entity>();

    for (int i = 0; i < 9; i++) list.add(new Entity(i, false));

    StepVerifier.create(repo.batchPut(list)).verifyComplete();

    StepVerifier.create(repo.query("hash", "sort").collectList())
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  final void testQueryIndex() {
    final List<Entity> list = new ArrayList<Entity>();

    for (int i = 0; i < 9; i++) list.add(new Entity(i, false));

    StepVerifier.create(repo.batchPut(list)).verifyComplete();
    StepVerifier.create(repo.queryIndex(0, 0, "index").collectList())
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  final void testBatchPut() {
    final List<Entity> list = new ArrayList<Entity>();

    for (int i = 0; i < 9; i++) list.add(new Entity(i, false));

    StepVerifier.create(repo.batchPut(list)).verifyComplete();
  }

  @Test
  final void testBatchDelete() {
    final List<Entity> list = new ArrayList<Entity>();

    for (int i = 0; i < 9; i++) list.add(new Entity(i, true));
    StepVerifier.create(repo.batchPut(list)).verifyComplete();
    StepVerifier.create(repo.batchDelete(list)).verifyComplete();
  }
}
