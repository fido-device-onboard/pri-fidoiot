// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import java.sql.Blob;
import java.sql.Clob;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


@Entity
@Table(name = "onboarding_voucher")
public class OnboardingVoucher {

  @Id
  @Column(name = "guid")
  private String guid;

  @Lob
  @Column(name = "data", nullable = false)
  private byte[] data;


  @Column(name = "created_on", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;


  public String getGuid() {
    return guid;
  }

  public byte[] getData() {
    return data;
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

  public void setCreatedOn(Date date ) {
    this.createdOn = date;
  }

}
