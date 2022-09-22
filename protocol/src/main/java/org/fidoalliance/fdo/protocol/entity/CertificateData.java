// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "certificate_data")
public class CertificateData {

  @Id
  @Column(name = "name", nullable = false)
  private String name;

  @Lob
  @Column(name = "data", length = 65535)
  private byte[] data;

  public String getName() {
    return name;
  }

  public byte[] getData() {
    return data;
  }

  public void setName(String name) {
    this.name = name;
  }


  public void setData(byte[] blob) {
    this.data = blob;
  }

}
