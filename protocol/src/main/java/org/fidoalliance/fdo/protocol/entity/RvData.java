// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import java.sql.Clob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "rv_data")
public class RvData {

  @Id
  @Column(name = "id", nullable = false)
  private long id = 1;

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
