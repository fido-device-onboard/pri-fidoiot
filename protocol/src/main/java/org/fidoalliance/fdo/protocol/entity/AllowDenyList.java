package org.fidoalliance.fdo.protocol.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "allow_deny_list")
public class AllowDenyList {

  @Id
  @Column(name = "hash")
  private String hash;

  @Column(name = "allowed")
  private boolean allowed;

  public String getHash() {
    return hash;
  }

  public boolean isAllowed() {
    return allowed;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public void setAllowed(boolean allowed) {
    this.allowed = allowed;
  }
}
