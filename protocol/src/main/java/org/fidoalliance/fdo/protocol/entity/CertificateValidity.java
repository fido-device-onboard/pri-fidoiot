// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "certificate_validity")
public class CertificateValidity {

  @Id
  @Column(name = "id")
  private final long id = 1;
  @Column(name = "days")
  private int days;


  public int getDays() {
    return days;
  }

  public void setDays(int days) {
    this.days = days;
  }

}
