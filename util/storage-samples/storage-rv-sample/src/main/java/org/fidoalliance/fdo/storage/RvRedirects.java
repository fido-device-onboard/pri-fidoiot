package org.fidoalliance.fdo.storage;

import java.sql.Blob;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "RV_REDIRECTS")
public class RvRedirects {

  @Id
  @Column(name = "GUID", length = 36, unique = true)
  private String guid;

  @Lob
  @Column(name = "REDIRECT_BLOB")
  private byte[] redirectBlob;

  @Column(name = "OWNER_KEY", length = 2048)
  private String ownerKey;

  @Column(name = "DEVICE_KEY", length = 2048)
  private byte[] deviceKey;

  @Column(name = "WAIT_SECONDS_RESPONSE")
  private Integer waitSecondsResponse;

  @Column(name = "CREATED")
  private Timestamp created;

  @Column(name = "EXPIRES_AT")
  private Timestamp expiresAt;

  public RvRedirects() {
  }

  /**
   * Construct a new RvRedirects field-by-field.
   */
  public RvRedirects(
      String guid,
      byte[] redirectBlob,
      String ownerKey,
      byte[] deviceKey,
      Integer waitSecondsResponse,
      Timestamp created,
      Timestamp expiresAt) {

    this.guid = guid;
    this.redirectBlob = redirectBlob;
    this.ownerKey = ownerKey;
    this.deviceKey = deviceKey;
    this.waitSecondsResponse = waitSecondsResponse;
    this.created = created;
    this.expiresAt = expiresAt;
  }

  public Timestamp getCreated() {
    return created;
  }

  public void setCreated(Timestamp at) {
    created = at;
  }

  public byte[] getDeviceKey() {
    return deviceKey;
  }

  public void setDeviceKey(byte[] in) {
    deviceKey = in;
  }

  public Timestamp getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Timestamp at) {
    expiresAt = at;
  }

  public String getGuid() {
    return guid;
  }

  public void setGuid(String s) {
    guid = s;
  }

  public String getOwnerKey() {
    return ownerKey;
  }

  public void setOwnerKey(String s) {
    ownerKey = s;
  }

  public byte[] getRedirectBlob() {
    return redirectBlob;
  }

  public void setRedirectBlob(byte[] b) {
    redirectBlob = b;
  }

  public Integer getWaitSecondsResponse() {
    return waitSecondsResponse;
  }

  public void setWaitSecondsResponse(Integer i) {
    waitSecondsResponse = i;
  }
}
