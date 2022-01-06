package org.fidoalliance.fdo.protocol.entity;


import java.sql.Blob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "certificate_data")
public class CertificateData {
  @Id
  @Column(name = "id", nullable = false)
  private String name;

  @Lob
  @Column(name = "data")
   private Blob data;

  public String getId() {
    return name;
  }

  public Blob getData() {
    return data;
  }

  public void setId(String name) {
    this.name = name;
  }

  public void setData(Blob blob) {
    this.data = blob;
  }
}
