// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.sql.Clob;

@Entity
@Table(name = "rv_data")
public class RvData {

  @Id
  @Column(name = "id", nullable = false)
  private final long id = 1;

  @Lob
  @Column(name = "data")
  private Clob data;


  public Clob getData() {
    return data;
  }

  public void setData(Clob data) {
    this.data = data;
  }


}
