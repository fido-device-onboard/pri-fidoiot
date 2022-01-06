package org.fidoalliance.fdo.protocol.entity;

import java.sql.Blob;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "resell_voucher")
public class ResellVoucher {

  private String id;
  private Blob data;

  @Id
  @Column(name = "id")
  public String getId() {
    return id;
  }

  @Column(name = "created_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;

  @Lob
  @Column(name = "data")
  public Blob getData() {
    return data;
  }

  public Date getCreatedOn() { return createdOn; }

  public void setId(String id) {
    this.id = id;
  }

  public void setCreatedOn(Date date ) {
    this.createdOn = date;
  }

  public void setData(Blob data) {
    this.data = data;
  }

}
