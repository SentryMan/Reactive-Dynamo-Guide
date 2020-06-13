package com.jojo.reactive.dynamo.repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.jojo.reactive.dynamo.config.DynamoEmbeddedConfig;
import com.jojo.reactive.dynamo.models.Entity;
import reactor.test.StepVerifier;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class TableRepositoryTest {


  @Autowired TableRepository repo;
  Entity entity = new Entity();

  @AfterAll
  static void tearDownAfterClass() throws Exception {
    DynamoEmbeddedConfig.stopEmbeddedDB();
  }

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
    List<Entity> list = new ArrayList<Entity>();
    
    for (int i = 0; i < 9; i++) {
     list.add(new Entity(i, false));
     
   } StepVerifier.create(repo.batchPut(list)).expectNextCount(1).verifyComplete();

    assert repo.query("hash", "sort").collectList().block() != null;
  }

  @Test
  final void testQueryIndex() {
    List<Entity> list = new ArrayList<Entity>();
    
    for (int i = 0; i < 9; i++) {
     list.add(new Entity(i, false));
     
   }
    
    StepVerifier.create(repo.batchPut(list)).expectNextCount(1).verifyComplete();

    assert repo.queryIndex(0, 0, "index").collectList().block() != null;
  }

  @Test
  final void testBatchPut() {
   List<Entity> list = new ArrayList<Entity>();
   
   for (int i = 0; i < 9; i++) {
    list.add(new Entity(i, false));
    
  }
   
   StepVerifier.create(repo.batchPut(list)).expectNextCount(1).verifyComplete();
  }

  @Test
  final void testBatchDelete() {
    List<Entity> list = new ArrayList<Entity>();
    
    for (int i = 0; i < 9; i++) {
     list.add(new Entity(i, true));
     
   }
    StepVerifier.create(repo.batchPut(list)).expectNextCount(1).verifyComplete();
    StepVerifier.create(repo.batchDelete(list)).expectNextCount(1).verifyComplete();
    
  }
}
