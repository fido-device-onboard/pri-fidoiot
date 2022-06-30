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
import java.util.Date;


@Entity
@Table(name = "manufactured_voucher")
public class ManufacturedVoucher {

  @Id
  @Column(name = "serial_no")
  private String serialNo;

  @Lob
  @Column(name = "data", length = 65535, nullable = false)
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
