// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "protocol_session")
public class ProtocolSession {

  @Id
  @Column(name = "name")
  private String name;

  @Lob
  @Column(name = "data")
  private byte[] data;

  @Column(name = "created_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;

  public String getName() {
    return name;
  }

  public byte[] getData() {
    return data;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public void setCreatedOn(Date date) {
    this.createdOn = date;
  }
}
