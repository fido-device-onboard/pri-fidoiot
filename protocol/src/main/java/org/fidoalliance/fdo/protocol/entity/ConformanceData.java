// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
