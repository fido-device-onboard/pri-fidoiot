// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import java.sql.Blob;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "manufactured_voucher")
public class ManufacturedVoucher {

  @Id
  @Column(name = "serial_no")
  private String serialNo;

  @Lob
  @Column(name = "data", nullable = false)
  private byte[] data;

  @Column(name = "created_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;

  public String getSerialNo() {
    return serialNo;
  }

  public byte[] getData() {
    return data;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setSerialNo(String id) {
    this.serialNo = id;
  }

  public void setCreatedOn(Date date) {
    this.createdOn = date;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

}
