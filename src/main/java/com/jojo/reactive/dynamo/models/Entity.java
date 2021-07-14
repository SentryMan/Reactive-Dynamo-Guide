package com.jojo.reactive.dynamo.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Entity {
  private String hashKey = "hash";
  private String sortKey = "sort";
  private int indexHashKey = 0;
  private int indexSortKey = 0;
  private CustomType obj = new CustomType();

  public Entity(int i, boolean bool) {
    if (bool) hashKey = hashKey + i;
    sortKey = sortKey + i;
    if (bool) indexHashKey = indexHashKey + i;
    indexSortKey = indexSortKey + i;
  }
}
