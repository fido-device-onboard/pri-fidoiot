// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SimpleSessionStorage implements SessionStorage {

  private final Map<Object, Value> map = new ConcurrentHashMap<>();

  private final Duration defaultTtl;

  public SimpleSessionStorage(Duration defaultTtl) {
    this.defaultTtl = defaultTtl;
  }

  public SimpleSessionStorage() {
    this.defaultTtl = Duration.ofMinutes(5);
  }

  @Override
  public Object load(Object key) {
    flush();
    Value value = getMap().get(key);

    if (null != value) {
      value.touch();
    }

    return null != value ? value.getValue() : null;
  }

  @Override
  public void store(Object key, Object value) {
    this.store(key, value, defaultTtl);
  }

  @Override
  public void store(Object key, Object value, Duration timeout) {
    getMap().put(key, new Value(value, timeout));
    flush();
  }

  @Override
  public void remove(Object key) {
    getMap().remove(key);
  }

  private void flush() {
    getMap().entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  private Map<Object, Value> getMap() {
    return map;
  }

  class Value {

    private final Duration timeout;
    private final Object value;
    private Instant expiresAt;

    Value(Object value, Duration timeout) {
      this.timeout = timeout;
      this.expiresAt = Instant.now().plus(timeout);
      this.value = value;
    }

    private Instant getExpiresAt() {
      return expiresAt;
    }

    private void setExpiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
    }

    private Duration getTimeout() {
      return timeout;
    }

    private Object getValue() {
      return value;
    }

    private boolean isExpired() {
      return (Instant.now().isAfter(getExpiresAt()));
    }

    private void touch() {
      this.expiresAt = Instant.now().plus(getTimeout());
    }
  }
}
