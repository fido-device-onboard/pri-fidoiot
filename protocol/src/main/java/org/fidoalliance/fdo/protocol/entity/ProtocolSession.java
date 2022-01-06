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
@Table(name = "protocol_session")
public class ProtocolSession {

  private String id;

  @Lob
  private Blob data;

  @Column(name = "created_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;

  @Id
  @Column(name = "id")
  public String getId() {
    return id;
  }

  @Lob
  @Column(name = "data")
  public Blob getData() {
    return data;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setId(String name) {
    this.id = name;
  }

  public void setData(Blob data) {
    this.data = data;
  }

  public void setCreatedOn(Date date) {
    this.createdOn = date;
  }
}
