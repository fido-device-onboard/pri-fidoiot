// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.sql.Blob;


@Entity
@Table(name = "system_resource")
public class SystemResource {

  @Id
  @Column(name = "name", nullable = false)
  private String name;

  @Lob
  @Column(name = "data", nullable = false)
  private Blob data;

  public String getName() {
    return name;
  }

  public Blob getData() {
    return data;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setData(Blob data) {
    this.data = data;
  }
}
