// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.sdo.cri.PublicKeyCodec.Encoder;

class DeviceCredentialsBuilder {

  private final DigestService digestService;
  private String deviceInfo = "NONE";
  private UUID guid = Uuids.buildRandomUuid();
  private SecretKey hmacKey = null;
  private PublicKey ownerKey = null;
  private RendezvousInfo rendezvousInfo = new RendezvousInfo();
  private DeviceState deviceState = DeviceState.READY1;

  public DeviceCredentialsBuilder(final DigestService digestService) {
    this.digestService = digestService;
  }

  /**
   * Build DeviceCredentials113.
   */
  public DeviceCredentials113 build() throws IOException {

    ManufacturerBlock m = buildManufacturerBlock();
    OwnerBlock o = buildOwnerBlock();

    return new DeviceCredentials113(getSt(), getHmacKey().getEncoded(), m, o);
  }

  /**
   * Build a ManufacturerBlock.
   */
  private ManufacturerBlock buildManufacturerBlock() {

    return new ManufacturerBlock(getD());
  }

  /**
   * Build an OwnerBlock.
   */
  private OwnerBlock buildOwnerBlock() throws IOException {

    KeyEncoding keyEncoding =
        getOwnerKey() instanceof RSAPublicKey ? KeyEncoding.RSAMODEXP : KeyEncoding.X_509;

    StringWriter keyWriter = new StringWriter();
    new Encoder(keyEncoding).encode(keyWriter, getOwnerKey());

    final HashDigest pkh = getDigestService().digestOf(US_ASCII.encode(keyWriter.toString()));

    UUID g = getG();
    if (null == g) { // null means 'create random UUIDs'
      g = Uuids.buildRandomUuid();
    }

    return new OwnerBlock(
        keyEncoding,
        g,
        getR(),
        pkh);
  }

  private String getD() {
    return deviceInfo;
  }

  public void setD(String d) {
    this.deviceInfo = d;
  }

  private UUID getG() {
    return guid;
  }

  public void setG(UUID g) {
    this.guid = g;
  }

  private RendezvousInfo getR() {
    return rendezvousInfo;
  }

  public void setR(RendezvousInfo r) {
    this.rendezvousInfo = r;
  }

  private DeviceState getSt() {
    return deviceState;
  }

  public void setSt(DeviceState st) {
    this.deviceState = st;
  }

  private SecretKey getHmacKey() {
    return hmacKey;
  }

  public void setHmacKey(SecretKey hmacKey) {
    this.hmacKey = hmacKey;
  }

  private PublicKey getOwnerKey() {
    return ownerKey;
  }

  public void setOwnerKey(PublicKey ownerKey) {
    this.ownerKey = ownerKey;
  }

  private DigestService getDigestService() {
    return digestService;
  }
}
