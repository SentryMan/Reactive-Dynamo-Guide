package com.jojo.reactive.dynamo.models;

import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Setter
public class EntityDAO {

  private String hashKey;
  private String sortKey;
  private int indexHashKey;
  private int indexSortKey;
  private CustomType obj;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(value = "hashKey")
  public String getHashKey() {
    return hashKey;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(value = "sortKey")
  public String getSortKey() {
    return sortKey;
  }

  @DynamoDbAttribute(value = "secondaryHashKey")
  @DynamoDbSecondaryPartitionKey(indexNames = {"index"})
  public int getIndexHashKey() {
    return indexHashKey;
  }

  @DynamoDbAttribute(value = "secondarySortKey")
  @DynamoDbSecondarySortKey(indexNames = {"index"})
  public int getIndexSortKey() {
    return indexSortKey;
  }

  public CustomType getObj() {
    return obj;
  }
}
