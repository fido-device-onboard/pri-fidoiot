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
  private long id;
  @Column(name = "days")
  private int days;

  public long getId() {
    return id;
  }

  public int getDays() {
    return days;
  }


  public void setId(long id) {
    this.id = id;
  }

  public void setDays(int days) {
    this.days = days;
  }

}
