package org.fidoalliance.fdo.protocol.entity;


import java.sql.Blob;
import java.sql.Clob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "rv_data")
public class RvData {
  private long id;
  private Blob data;

  @Id
  @Column(name = "id")
  public long getId() {
    return id;
  }

  @Lob
  @Column(name = "data")
  public Blob getData() {
    return data;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setData(Blob data) {
    this.data = data;
  }
}
