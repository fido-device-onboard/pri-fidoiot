// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.CwtTokenDeserializer;
import org.fidoalliance.fdo.protocol.serialization.CwtTokenSerializer;

@JsonSerialize(using = CwtTokenSerializer.class)
@JsonDeserialize(using = CwtTokenDeserializer.class)
public class CwtToken {

  private String issuer;

  private String subject;

  private String audience;

  private long expiry;

  private long notBefore;

  private long issuedAt;

  private byte[] cwtId;

  public String getIssuer() {
    return issuer;
  }

  public String getSubject() {
    return subject;
  }

  public String getAudience() {
    return audience;
  }

  public long getExpiry() {
    return expiry;
  }

  public long getNotBefore() {
    return notBefore;
  }

  public long getIssuedAt() {
    return issuedAt;
  }

  public byte[] getCwtId() {
    return cwtId;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public void setAudience(String audience) {
    this.audience = audience;
  }

  public void setExpiry(long expiry) {
    this.expiry = expiry;
  }

  public void setNotBefore(long notBefore) {
    this.notBefore = notBefore;
  }

  public void setIssuedAt(long issuedAt) {
    this.issuedAt = issuedAt;
  }

  public void setCwtId(byte[] cwtId) {
    this.cwtId = cwtId;
  }
}
