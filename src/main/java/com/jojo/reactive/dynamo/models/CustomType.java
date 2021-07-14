package com.jojo.reactive.dynamo.models;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@DynamoDbBean
public class CustomType {

  String s = "string";
  int i = 0;
}
