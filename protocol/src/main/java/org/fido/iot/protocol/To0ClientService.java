// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.IOException;
import java.security.PublicKey;

/**
 * To0 Client message processing service.
 */
public abstract class To0ClientService extends ClientService {

  protected Composite voucher;

  protected abstract To0ClientStorage getStorage();

  protected void doHelloAck(Composite request, Composite reply) {
    getStorage().starting(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    byte[] nonce3 = body.getAsBytes(Const.FIRST_KEY);
    if (nonce3.length != Const.NONCE16_SIZE) {
      throw new MessageBodyException();
    }

    CryptoService cryptoService = getCryptoService();

    Composite voucher = getStorage().getVoucher();

    Composite to0d = Composite.newArray()
        .set(Const.TO0D_VOUCHER, voucher)
        .set(Const.TO0D_WAIT_SECONDS, getStorage().getRequestWait())
        .set(Const.TO0D_NONCE3, nonce3);

    Composite to1dBlob = getStorage().getRedirectBlob();
    Composite to01Payload = Composite.newArray()
        .set(Const.TO1D_RV, to1dBlob);

    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    PublicKey mfgPublic = cryptoService.decode(ovh.getAsComposite(Const.OVH_PUB_KEY));
    int hashType = cryptoService.getCompatibleHashType(mfgPublic);
    Composite hash = cryptoService.hash(hashType, to0d.toBytes());

    to01Payload.set(Const.TO1D_TO0D_HASH, hash);

    Composite pubEncKey = getCryptoService().getOwnerPublicKey(voucher);
    PublicKey ownerPublicKey = getCryptoService().decode(pubEncKey);
    Composite singedBlob = null;
    try (CloseableKey key = new CloseableKey(
        getStorage().getOwnerSigningKey(ownerPublicKey))) {
      singedBlob = getCryptoService().sign(key.get(), to01Payload.toBytes());
    } catch (IOException e) {
      throw new DispatchException(e);
    }

    Composite ownerSign = Composite.newArray()
        .set(Const.TO0_TO0D, to0d)
        .set(Const.TO0_TO1D, singedBlob);

    reply.set(Const.SM_MSG_ID, Const.TO0_OWNER_SIGN);
    reply.set(Const.SM_BODY, ownerSign);
    getStorage().started(request, reply);
  }

  protected void doAcceptOwner(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    body.verifyMaxKey(Const.FIRST_KEY);
    long responseWait = body.getAsNumber(Const.FIRST_KEY).longValue();
    if (responseWait > Const.MAX_UINT32) {
      throw new InvalidMessageException(new IllegalArgumentException());
    }
    getStorage().setResponseWait(responseWait);
    reply.clear();
    getStorage().completed(request, reply);
  }

  protected void doError(Composite request, Composite reply) {
    reply.clear();
    getStorage().failed(request, reply);
  }

  @Override
  public boolean dispatch(Composite request, Composite reply) {
    switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
      case Const.TO0_HELLO_ACK:
        doHelloAck(request, reply);
        return false;
      case Const.TO0_ACCEPT_OWNER:
        doAcceptOwner(request, reply);
        return true;
      case Const.ERROR:
        doError(request, reply);
        return true;
      default:
        throw new RuntimeException(new UnsupportedOperationException());
    }
  }

  @Override
  public DispatchResult getHelloMessage() {

    voucher = getStorage().getVoucher();
    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);

    getStorage().starting(Const.EMPTY_MESSAGE, Const.EMPTY_MESSAGE);
    DispatchResult dr = new DispatchResult(Composite.newArray()
        .set(Const.SM_LENGTH, Const.DEFAULT)
        .set(Const.SM_MSG_ID, Const.TO0_HELLO)
        .set(Const.SM_PROTOCOL_VERSION,
            ovh.getAsNumber(Const.OVH_VERSION).intValue())
        .set(Const.SM_PROTOCOL_INFO, Composite.fromObject(Const.EMPTY_BYTE))
        .set(Const.SM_BODY, Const.EMPTY_MESSAGE), false);
    getStorage().started(Const.EMPTY_MESSAGE, dr.getReply());
    return dr;
  }
}
