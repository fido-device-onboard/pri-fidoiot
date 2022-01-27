package org.fidoalliance.fdo.protocol;

public enum CertificateType {
  RSA_2048_RESTR(0),
  RSA_2048_PKCS(1),
  RSA_3072_PKCS(2),
  EC2_P256(3),
  EC2_P384(4);

  private int id;

  CertificateType(int id) {
    this.id = id;
  }


  public int toInteger() {
    return id;
  }

}
