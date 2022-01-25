// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "certificate_validity")
public class CertificateValidity {

  @Id
  @Column(name = "id")
  private long id = 1;
  @Column(name = "days")
  private int days;


  public int getDays() {
    return days;
  }

  public void setDays(int days) {
    this.days = days;
  }

}
