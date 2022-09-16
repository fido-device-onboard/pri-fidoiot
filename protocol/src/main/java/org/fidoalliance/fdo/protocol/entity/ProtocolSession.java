// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.sql.Blob;
import java.util.Date;

@Entity
@Table(name = "protocol_session")
public class ProtocolSession {

  @Id
  @Column(name = "name")
  private String name;

  @Lob
  @Column(name = "data")
  private Blob data;

  @Column(name = "created_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;

  public String getName() {
    return name;
  }

  public Blob getData() {
    return data;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setData(Blob data) {
    this.data = data;
  }

  public void setCreatedOn(Date date) {
    this.createdOn = date;
  }
}
