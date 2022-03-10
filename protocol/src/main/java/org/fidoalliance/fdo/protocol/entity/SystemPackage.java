// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import java.sql.Clob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "system_package")
public class SystemPackage {

  @Id
  @Column(name = "id", nullable = false)
  private long id = 1;

  @Lob
  @Column(name = "data", nullable = false)
  private Clob data;

  public long getId() {
    return id;
  }

  public Clob getData() {
    return data;
  }

  public void setData(Clob data) {
    this.data = data;
  }

}
