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
