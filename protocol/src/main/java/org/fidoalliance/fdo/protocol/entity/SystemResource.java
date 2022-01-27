package org.fidoalliance.fdo.protocol.entity;

import java.sql.Blob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;


@Entity
@Table(name = "system_resource")
public class SystemResource {

  @Id
  @Column(name = "name", nullable = false)
  private String name;

  @Lob
  @Column(name = "data", nullable = false)
  private Blob data;

  public String getName() {
    return name;
  }

  public Blob getData() {
    return data;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setData(Blob data) {
    this.data = data;
  }
}
