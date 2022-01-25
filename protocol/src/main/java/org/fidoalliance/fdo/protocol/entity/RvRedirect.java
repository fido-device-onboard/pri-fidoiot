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
@Table(name = "rv_redirect")
public class RvRedirect {

  @Id
  @Column(name = "guid", nullable = false)
  private String guid;

  @Lob
  @Column(name = "data", nullable = false)
  private byte[] data;

  @Column(name = "expiry", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date expiry;


  @Column(name = "created_on", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;

  public String getGuid() {
    return guid;
  }

  public byte[] getData() {
    return data;
  }

  public Date getExpiry() {
    return expiry;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public void setExpiry(Date expiry) {
    this.expiry = expiry;
  }

  public void setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
  }
}
