// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.UUID;
import javax.crypto.SecretKey;

class VoucherBuilder {

  private String deviceInfo = "NONE";
  private CertPath dc = null;
  private UUID uuid = Uuids.buildRandomUuid();
  private SecretKey hmacKey = null;
  private PublicKey ownerKey = null;
  private RendezvousInfo rendezvousInfo = new RendezvousInfo();
  private final DigestService digestService;

  public VoucherBuilder(DigestService digestService) {
    this.digestService = digestService;
  }

  /**
   * Build an OwnershipVoucher113.
   */
  public OwnershipVoucher113 build() throws IOException {

    HashDigest hdc = null;

    CertPath dc = getDc();
    if (null != dc) {
      StringWriter writer = new StringWriter();
      new CertPathCodec().encoder().apply(writer, dc);
      hdc = digestService.digestOf(US_ASCII.encode(writer.toString()));
    }

    UUID g = getG();
    if (null == g) {
      g = Uuids.buildRandomUuid();
    }

    OwnershipVoucherHeader oh = new OwnershipVoucherHeader(
        getOwnerKey() instanceof RSAPublicKey ? KeyEncoding.RSAMODEXP : KeyEncoding.X_509,
        getR(),
        g,
        getD(),
        getOwnerKey(),
        hdc);

    StringWriter writer = new StringWriter();
    new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderEncoder().encode(writer, oh);

    HashMac hmac;
    if (null != getHmacKey()) { // this can be empty if DI isn't finished yet
      hmac = new SimpleMacService(getHmacKey()).macOf(US_ASCII.encode(writer.toString()));

    } else {
      hmac = new HashMac(MacType.NONE, ByteBuffer.allocate(0));
    }

    return new OwnershipVoucher113(oh, hmac, dc, new ArrayList<>());
  }

  private String getD() {
    return deviceInfo;
  }

  public void setD(String d) {
    this.deviceInfo = d;
  }

  private CertPath getDc() {
    return dc;
  }

  public void setDc(CertPath dc) {
    this.dc = dc;
  }

  private UUID getG() {
    return uuid;
  }

  public void setG(UUID g) {
    this.uuid = g;
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

  private RendezvousInfo getR() {
    return rendezvousInfo;
  }

  public void setR(RendezvousInfo r) {
    this.rendezvousInfo = r;
  }
}
