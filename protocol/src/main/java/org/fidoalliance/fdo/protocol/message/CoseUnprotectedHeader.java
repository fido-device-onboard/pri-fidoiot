package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.CoseUnprotectedHeaderDeserializer;
import org.fidoalliance.fdo.protocol.serialization.CoseUnprotectedHeaderSerializer;


@JsonSerialize(using = CoseUnprotectedHeaderSerializer.class)
@JsonDeserialize(using = CoseUnprotectedHeaderDeserializer.class)
public class CoseUnprotectedHeader {

  private Nonce nonce;
  private OwnerPublicKey ownerPublicKey;
  private byte[]  maroPrefix;
  private byte[] iv;

  public Nonce getNonce() {
    return nonce;
  }

  public OwnerPublicKey getOwnerPublicKey() {
    return ownerPublicKey;
  }

  public byte[] getMaroPrefix() {
    return maroPrefix;
  }

  public byte[] getIv() {
    return iv;
  }

  public void setNonce(Nonce nonce) {
    this.nonce = nonce;
  }

  public void setOwnerPublicKey(OwnerPublicKey ownerPublicKey) {
    this.ownerPublicKey = ownerPublicKey;
  }

  public void setMaroPrefix(byte[] maroPrefix) {
    this.maroPrefix = maroPrefix;
  }

  public void setIv(byte[] iv) {
    this.iv = iv;
  }
}
