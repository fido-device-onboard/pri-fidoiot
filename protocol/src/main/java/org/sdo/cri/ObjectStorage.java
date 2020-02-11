// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.util.Optional;

/**
 * An interface for an object which can load and store others.
 *
 * @param <K> the type of the storage keys
 * @param <V> the type of the storage values
 */
public interface ObjectStorage<K, V extends Serializable> {

  /**
   * Load an object described by the given key.
   *
   * @param key the key
   * @return the matching object, or null if no match exists.
   */
  Optional<V> load(K key);

  /**
   * Store an object.
   *
   * @param key the key under which to store the object
   * @param value the object to store
   */
  void store(K key, V value);
}
