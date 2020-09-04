// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Extends Ownership vouchers.
 */
public class VoucherExtensionService {

  private final Composite voucher;
  private final CryptoService cryptoService;

  /**
   * Constructs an VoucherExtensionService.
   *
   * @param voucher       The ownership voucher to extend.
   * @param cryptoService The crypto service to use.
   */
  public VoucherExtensionService(Composite voucher, CryptoService cryptoService) {
    this.voucher = voucher;
    this.cryptoService = cryptoService;
  }

  protected void add100(PrivateKey prevOwner, PublicKey newOwner) {

    Composite mac = voucher.getAsComposite(Const.OV_HMAC);
    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    int hashType = cryptoService.getCompatibleHashType(newOwner);

    byte[] guid = ovh.getAsBytes(Const.OVH_GUID);
    byte[] devInfo = ovh.getAsBytes(Const.OVH_DEVICE_INFO);
    ByteBuffer headerInfo = ByteBuffer.allocate(guid.length + devInfo.length);
    headerInfo.put(guid);
    headerInfo.put(devInfo);
    headerInfo.flip();
    Composite hdrHash = cryptoService.hash(hashType, Composite.unwrap(headerInfo));
    Composite prevHash;
    //For the first entry, the hash is SHA[TO2.ProveOPHdr.OVHeader|\|TO2.ProveOPHdr.HMac].
    Composite entries = voucher.getAsComposite(Const.OV_ENTRIES);
    if (entries.size() == 0) {
      byte[] ovhBytes = ovh.toBytes();
      byte[] macBytes = mac.toBytes();
      ByteBuffer prevInfo = ByteBuffer.allocate(ovhBytes.length + macBytes.length);
      prevInfo.put(ovhBytes);
      prevInfo.put(macBytes);
      prevInfo.flip();
      prevHash = cryptoService.hash(hashType, Composite.unwrap(prevInfo));
    } else {
      Composite prev = entries.getAsComposite(entries.size() - 1);
      Composite prevEntry = Composite.fromObject(prev.getAsBytes(Const.COSE_SIGN1_PAYLOAD));
      prevHash = prevEntry.getAsComposite(Const.OVE_HASH_PREV_ENTRY);
      hdrHash = cryptoService.hash(hashType, prev.toBytes());
    }

    Composite payload = Composite.newArray();

    payload.set(Const.OVE_HASH_PREV_ENTRY, prevHash);
    payload.set(Const.OVE_HASH_HDR_INFO, hdrHash);
    payload.set(Const.OVE_PUB_KEY, cryptoService.encode(newOwner, Const.PK_ENC_COSEEC));
    Composite cos = cryptoService.sign(prevOwner, payload.toBytes());

    //The current recommended maximum is ten entries.
    entries.set(entries.size(), cos);
  }

  /**
   * Adds a new owner to the voucher.
   *
   * @param prevOwner The private key of the previous owner.
   * @param newOwner  The public key of the new owner.
   */
  public void add(PrivateKey prevOwner, PublicKey newOwner) {
    Composite header = voucher.getAsComposite(Const.OV_HEADER);
    switch (header.getAsNumber(Const.OVH_VERSION).intValue()) {
      case Const.PROTOCOL_VERSION_100:
        add100(prevOwner, newOwner);
        break;
      default:
        throw new RuntimeException(new UnsupportedOperationException());
    }
  }

}
