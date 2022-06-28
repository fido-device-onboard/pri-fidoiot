// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "allow_deny_list")
public class AllowDenyList {

  @Id
  @Column(name = "hash")
  private String hash;

  @Column(name = "allowed")
  private boolean allowed;

  public String getHash() {
    return hash;
  }

  public boolean isAllowed() {
    return allowed;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public void setAllowed(boolean allowed) {
    this.allowed = allowed;
  }
}
