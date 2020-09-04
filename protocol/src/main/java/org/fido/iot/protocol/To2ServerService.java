// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

/**
 * To2 Server message processing service.
 */
public abstract class To2ServerService extends MessagingService {

  protected abstract To2ServerStorage getStorage();

  protected void doHello(Composite request, Composite reply) {
    getStorage().starting(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);

    UUID guid = body.getAsUuid(Const.FIRST_KEY);
    byte[] nonce5 = body.getAsBytes(Const.SECOND_KEY);
    String kexName = body.getAsString(Const.THIRD_KEY);
    String cipherName = body.getAsString(Const.FOURTH_KEY);
    Composite sigInfoA = body.getAsComposite(Const.FIFTH_KEY);

    Composite ownerState = getCryptoService()
        .getKeyExchangeMessage(kexName, Const.KEY_EXCHANGE_A);

    getStorage().setOwnerState(ownerState);
    getStorage().setCipherName(cipherName);
    getStorage().setGuid(guid);

    Composite voucher = getStorage().getVoucher();

    Composite payload = Composite.newArray()
        .set(Const.FIRST_KEY, voucher.getAsComposite(Const.OV_HEADER))
        .set(Const.SECOND_KEY, voucher.getAsComposite(Const.OV_ENTRIES).size())
        .set(Const.THIRD_KEY, voucher.getAsComposite(Const.OV_HMAC))
        .set(Const.FOURTH_KEY, nonce5)
        .set(Const.FIFTH_KEY, getStorage().getSigInfoB(sigInfoA))
        .set(Const.SIXTH_KEY, ownerState.getAsBytes(Const.FIRST_KEY));

    byte[] nonce6 = getCryptoService().getRandomBytes(Const.NONCE16_SIZE);
    getStorage().setNonce6(nonce6);
    Composite ownerKey = getCryptoService().getOwnerPublicKey(voucher);
    Composite uph = Composite.newMap()
        .set(Const.CUPH_NONCE, nonce6)
        .set(Const.CUPH_PUBKEY, ownerKey);

    Composite pubEncKey = getCryptoService().getOwnerPublicKey(voucher);
    PublicKey ownerPublicKey = getCryptoService().decode(pubEncKey);
    Composite cose = null;
    try (CloseableKey key = new CloseableKey(
        getStorage().geOwnerSigningKey(ownerPublicKey))) {
      cose = getCryptoService().sign(key.get(), payload.toBytes());
    } catch (IOException e) {
      throw new DispatchException(e);
    }

    cose.set(Const.COSE_SIGN1_UNPROTECTED, uph);
    reply.set(Const.SM_MSG_ID, Const.TO2_PROVE_OVHDR);
    reply.set(Const.SM_BODY, cose);
    getStorage().started(request, reply);

  }

  protected void doGetOpEntry(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    int entryNum = body.getAsNumber(Const.FIRST_KEY).intValue();
    Composite voucher = getStorage().getVoucher();
    Composite entries = voucher.getAsComposite(Const.OV_ENTRIES);
    if (entryNum < 0 || entryNum >= entries.size()) {
      throw new InvalidMessageException();
    }

    Composite entry = entries.getAsComposite(entryNum);

    body = Composite.newArray()
        .set(Const.FIRST_KEY, entryNum)
        .set(Const.SECOND_KEY, entry);

    reply.set(Const.SM_MSG_ID, Const.TO2_OVNEXT_ENTRY);
    reply.set(Const.SM_BODY, body);
    getStorage().continued(request, reply);
  }

  protected void doProveDevice(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);

    Composite voucher = getStorage().getVoucher();
    PublicKey deviceKey = getCryptoService().getDevicePublicKey(voucher);
    if (!getCryptoService().verify(deviceKey, body)) {
      throw new InvalidMessageException();
    }

    Composite payload = Composite.fromObject(
        body.getAsBytes(Const.COSE_SIGN1_PAYLOAD));

    Composite iotClaim = payload.getAsComposite(Const.EAT_SDO_IOT);
    byte[] kexB = iotClaim.getAsBytes(Const.FIRST_KEY);

    byte[] devSecret = getCryptoService().getSharedSecret(kexB,
        getStorage().getOwnerState());
    Composite cipherState = getCryptoService().getEncryptionState(devSecret,
        getStorage().getCipherName());
    getStorage().setOwnerState(cipherState);

    Composite unp = body.getAsComposite(Const.COSE_SIGN1_UNPROTECTED);
    byte[] nonce7 = unp.getAsBytes(Const.EUPH_NONCE);
    getStorage().setNonce7(nonce7);

    payload = Composite.newArray();
    payload.set(Const.FIRST_KEY, getStorage().getReplacementRvInfo());
    payload.set(Const.SECOND_KEY, getStorage().getReplacementGuid());
    payload.set(Const.THIRD_KEY, getStorage().getNonce6());
    payload.set(Const.FOURTH_KEY, getStorage().getReplacementOwnerKey());

    body = getCryptoService().encrypt(
        payload.toBytes(),
        getStorage().getOwnerState());
    reply.set(Const.SM_MSG_ID, Const.TO2_SETUP_DEVICE);
    reply.set(Const.SM_BODY, body);
    getStorage().continued(request, reply);
  }

  protected void doAuthDone(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    Composite message = Composite.fromObject(getCryptoService().decrypt(body,
        getStorage().getOwnerState()));

    Object hmacObj = message.get(Const.FIRST_KEY);
    if (!hmacObj.equals(Optional.empty())) {
      Composite hmac = Composite.fromObject(hmacObj);
      getStorage().setReplacementHmac(hmac);
    }

    Composite payload = Const.EMPTY_MESSAGE;
    body = getCryptoService().encrypt(payload.toBytes(),
        getStorage().getOwnerState());
    reply.set(Const.SM_MSG_ID, Const.TO2_AUTH_DONE2);
    reply.set(Const.SM_BODY, body);

    getStorage().prepareServiceInfo();
    getStorage().continued(request, reply);
  }

  protected void doDeviceInfo(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    Composite message =
        Composite.fromObject(
            getCryptoService().decrypt(body, getStorage().getOwnerState()));

    boolean isMore = message.getAsBoolean(Const.FIRST_KEY);
    Composite sviValues = message.getAsComposite(Const.SECOND_KEY);

    for (int i = 0; i < sviValues.size(); i++) {
      Composite sviValue = sviValues.getAsComposite(i);
      getStorage().setServiceInfo(sviValue, isMore);
    }

    Composite payload = getStorage().getNextServiceInfo();

    body = getCryptoService().encrypt(payload.toBytes(), getStorage().getOwnerState());
    reply.set(Const.SM_MSG_ID, Const.TO2_OWNER_SERVICE_INFO);
    reply.set(Const.SM_BODY, body);
    getStorage().continued(request, reply);
  }

  protected void doDone(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    Composite message = Composite.fromObject(
        getCryptoService().decrypt(body, getStorage().getOwnerState()));
    byte[] nonce6 = message.getAsBytes(Const.FIRST_KEY);

    getCryptoService().verifyBytes(nonce6, getStorage().getNonce6());

    Composite payload = Composite.newArray()
        .set(Const.FIRST_KEY, getStorage().getNonce7());

    body = getCryptoService().encrypt(
        payload.toBytes(),
        getStorage().getOwnerState());
    reply.set(Const.SM_MSG_ID, Const.TO2_DONE2);
    reply.set(Const.SM_BODY, body);
    getStorage().completed(request, reply);
  }

  protected void doError(Composite request, Composite reply) {
    reply.clear();
    getStorage().failed(request, reply);
  }

  @Override
  public boolean dispatch(Composite request, Composite reply) {
    switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
      case Const.TO2_HELLO_DEVICE:
        doHello(request, reply);
        return false;
      case Const.TO2_GET_OVNEXT_ENTRY:
        doGetOpEntry(request, reply);
        return false;
      case Const.TO2_PROVE_DEVICE:
        doProveDevice(request, reply);
        return false;
      case Const.TO2_AUTH_DONE:
        doAuthDone(request, reply);
        return false;
      case Const.TO2_DEVICE_SERVICE_INFO:
        doDeviceInfo(request, reply);
        return false;
      case Const.TO2_DONE:
        doDone(request, reply);
        return true;
      case Const.ERROR:
        doError(request, reply);
        return true;
      default:
        throw new RuntimeException(new UnsupportedOperationException());
    }
  }

}
