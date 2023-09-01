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
@Table(name = "voucher_alias")
public class VoucherAlias {

  @Id
  @Column(name = "alias")
  private String alias;

  @Column(name = "guid")
  private String guid;


  public String getAlias() {
    return alias;
  }

  public String getGuid() {
    return guid;
  }


  public void setAlias(String alias) {
    this.alias = alias;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }


}
