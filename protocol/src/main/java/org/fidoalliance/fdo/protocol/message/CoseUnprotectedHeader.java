package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.CoseUnprotectedHeaderDeserializer;
import org.fidoalliance.fdo.protocol.serialization.CoseUnprotectedHeaderSerializer;


@JsonSerialize(using = CoseUnprotectedHeaderSerializer.class)
@JsonDeserialize(using = CoseUnprotectedHeaderDeserializer.class)
public class CoseUnprotectedHeader {

  private Nonce cupNonce;
  private Nonce eatNone;
  private OwnerPublicKey ownerPublicKey;
  private byte[]  maroPrefix;
  private byte[] iv;

  public Nonce getCupNonce() {
    return cupNonce;
  }

  public Nonce getEatNonce() { return eatNone; }

  public OwnerPublicKey getOwnerPublicKey() {
    return ownerPublicKey;
  }

  public byte[] getMaroPrefix() {
    return maroPrefix;
  }

  public byte[] getIv() {
    return iv;
  }

  public void setCupNonce(Nonce nonce) {
    this.cupNonce = nonce;
  }

  public void setEatNone(Nonce nonce) {
    this.eatNone = nonce;
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
