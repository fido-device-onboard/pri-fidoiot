// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.fidoalliance.fdo.protocol.serialization.SimpleStorageDeserializer;
import org.fidoalliance.fdo.protocol.serialization.SimpleStorageSerializer;

/**
 * Provides simple storage of messages.
 */
@JsonSerialize(using = SimpleStorageSerializer.class)
@JsonDeserialize(using = SimpleStorageDeserializer.class)
public class SimpleStorage {

  private final Map<String, Object> map;

  /**
   * Default constructor.
   */
  @JsonIgnore
  public SimpleStorage() {
    map = new HashMap<>();
  }

  /**
   * Add on object to the storage.
   *
   * @param clazz The object name.
   * @param value The object to put in the storage.
   * @param <T>   The template type argument.
   * @return The Object replaced.
   */
  @JsonIgnore
  public <T> T put(Class<?> clazz, Object value) {
    return (T) map.put(clazz.getName(), value);
  }

  /**
   * Gets the object by its class type.
   *
   * @param clazz The object name to query.
   * @param <T>   The template type argument.
   * @return An instance of the object in storage or null if not found.
   */
  @JsonIgnore
  public <T> T get(Class<T> clazz) {
    return (T) map.get(clazz.getName());
  }


  /**
   * Gets the values in the storage.
   *
   * @return A collection of objects in the storage.
   */
  @JsonIgnore
  public Collection<Object> values() {
    return map.values();
  }


}
