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
@Table(name = "onboarding_voucher")
public class OnboardingVoucher {

  @Id
  @Column(name = "guid")
  private String guid;

  @Lob
  @Column(name = "data", nullable = false)
  private byte[] data;

  @Lob
  @Column(name = "replacement")
  private byte[] replacement;


  @Column(name = "created_on", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;

  @Column(name = "to2_completed_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date to2CompletedOn;

  @Column(name = "to0_expiry")
  @Temporal(TemporalType.TIMESTAMP)
  private Date to0Expiry;


  public String getGuid() {
    return guid;
  }

  public byte[] getData() {
    return data;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public Date getTo2CompletedOn() {
    return to2CompletedOn;
  }

  public Date getTo0Expiry() {
    return to0Expiry;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public void setCreatedOn(Date date) {
    this.createdOn = date;
  }

  public void setTo2CompletedOn(Date to2CompletedOn) {
    this.to2CompletedOn = to2CompletedOn;
  }

  public void setTo0Expiry(Date to0Expiry) {
    this.to0Expiry = to0Expiry;
  }

  public byte[] getReplacement() {
    return replacement;
  }

  public void setReplacement(byte[] replacement) {
    this.replacement = replacement;
  }
}
