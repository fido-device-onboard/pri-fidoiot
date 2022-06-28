// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conformance_data")
public class ConformanceData {

  @Id
  @Column(name = "guid")
  private String guid;

  @Column(name = "data", nullable = false)
  private String data;

  public String getGuid() {
    return guid;
  }

  public String getData() {
    return data;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public void setData(String data) {
    this.data = data;
  }

}
