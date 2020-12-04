// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECKey;

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
    PublicKey prevOwnerPubKey;
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
      prevOwnerPubKey = cryptoService.decode(voucher.getAsComposite(Const.OV_HEADER)
          .getAsComposite(Const.OVH_PUB_KEY));
    } else {
      Composite prev = entries.getAsComposite(entries.size() - 1);
      Composite prevEntry = Composite.fromObject(prev.getAsBytes(Const.COSE_SIGN1_PAYLOAD));
      prevHash = cryptoService.hash(hashType, prev.toBytes());
      prevOwnerPubKey = cryptoService.decode(prevEntry.getAsComposite(Const.OVE_PUB_KEY));
    }

    Composite payload = Composite.newArray();

    payload.set(Const.OVE_HASH_PREV_ENTRY, prevHash);
    payload.set(Const.OVE_HASH_HDR_INFO, hdrHash);
    Composite ownerPubkey = ovh.getAsComposite(Const.OVH_PUB_KEY);
    final int ownerKeyEnc = ownerPubkey.getAsNumber(Const.PK_ENC).intValue();
    payload.set(Const.OVE_PUB_KEY, cryptoService.encode(
            newOwner,
            ownerKeyEnc));
    Composite cos = cryptoService.sign(
        prevOwner, payload.toBytes(), cryptoService.getCoseAlgorithm(prevOwnerPubKey));

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
