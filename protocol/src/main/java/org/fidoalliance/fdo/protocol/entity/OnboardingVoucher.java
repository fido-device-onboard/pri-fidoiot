package org.fidoalliance.fdo.protocol.entity;

import java.sql.Blob;
import java.sql.Clob;
import javax.persistence.Column;
import javax.persistence.Id;

public class OnboardingVoucher {
  private String id;
  private String alias;
  private Blob data;

  @Id
  @Column(name = "id")
  public String getId() {
    return id;
  }

  @Column(name = "data")
  public Blob getData() {
    return data;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setData(Clob info) {
    this.data = data;
  }

}
