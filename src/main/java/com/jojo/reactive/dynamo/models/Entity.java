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
  private int secondaryHashKey = 0;
  private int secondarySortKey = 0;

  public Entity(int i, boolean bool) {
    if (bool) hashKey = hashKey + i;
    sortKey = sortKey + i;
    if (bool) secondaryHashKey = secondaryHashKey + i;
    secondarySortKey = secondarySortKey + i;
  }
}
