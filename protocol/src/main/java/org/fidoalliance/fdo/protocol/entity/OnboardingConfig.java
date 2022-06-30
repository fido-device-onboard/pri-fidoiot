// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.sql.Clob;


@Entity
@Table(name = "onboarding_config")
public class OnboardingConfig {

  @Id
  @Column(name = "id", nullable = false)
  private final long id = 1;

  @Lob
  @Column(name = "rv_blob", nullable = false)
  private Clob rvBlob;

  @Lob
  @Column(name = "replacement_rvInfo",length = 65535)
  private Clob replacementRvInfo;

  @Column(name = "max_message_size")
  private Integer maxMessageSize;

  @Column(name = "max_serviceinfo_size")
  private Integer maxServiceInfoSize;

  @Column(name = "wait_seconds", nullable = false)
  private long waitSeconds;

  public Clob getRvBlob() {
    return rvBlob;
  }

  public Clob getReplacementRvInfo() {
    return replacementRvInfo;
  }

  public Integer getMaxMessageSize() {
    return maxMessageSize;
  }

  public Integer getMaxServiceInfoSize() {
    return maxServiceInfoSize;
  }

  public long getWaitSeconds() {
    return waitSeconds;
  }


  public void setRvBlob(Clob rvBlob) {
    this.rvBlob = rvBlob;
  }

  public void setReplacementRvInfo(Clob replacementRvInfo) {
    this.replacementRvInfo = replacementRvInfo;
  }

  public void setMaxMessageSize(Integer maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }

  public void setMaxServiceInfoSize(Integer maxServiceInfoSize) {
    this.maxServiceInfoSize = maxServiceInfoSize;
  }

  public void setWaitSeconds(long waitSeconds) {
    this.waitSeconds = waitSeconds;
  }


}