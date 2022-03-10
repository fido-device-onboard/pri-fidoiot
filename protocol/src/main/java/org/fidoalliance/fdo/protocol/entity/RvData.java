// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

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
  private byte[] data;


  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }


}
