// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PublicKey;
import java.util.UUID;

/**
 * To1 Server message processing service.
 */
public abstract class To1ServerService extends MessagingService {

  public abstract To1ServerStorage getStorage();

  protected void doHello(Composite request, Composite reply) {

    getStorage().starting(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);

    UUID guid = body.getAsUuid(Const.FIRST_KEY);
    body.verifyMaxKey(Const.SECOND_KEY);

    getStorage().setGuid(guid);

    byte[] nonce4 = getCryptoService().getRandomBytes(Const.NONCE16_SIZE);
    getStorage().setNonce4(nonce4);
    Composite sigA = body.getAsComposite(Const.SECOND_KEY);

    body = Composite.newArray()
        .set(Const.FIRST_KEY, nonce4)
        .set(Const.SECOND_KEY,
            getStorage().getSigInfoB(sigA));

    reply.set(Const.SM_MSG_ID, Const.TO1_HELLO_RV_ACK);
    reply.set(Const.SM_BODY, body);
    getStorage().started(request, reply);
  }

  protected void doProveOwner(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    CryptoService cryptoService = getCryptoService();
    PublicKey deviceKey = getStorage().getVerificationKey();

    if (!cryptoService.verify(deviceKey, body)) {
      throw new InvalidMessageException();
    }
    Composite payload = Composite.fromObject(
        body.getAsBytes(Const.COSE_SIGN1_PAYLOAD));

    byte[] nonce4 = payload.getAsBytes(Const.EAT_NONCE);
    UUID ueGuid = getCryptoService().getGuidFromUeid(
        payload.getAsBytes(Const.EAT_UEID));
    UUID deviceId = getStorage().getGuid();

    if (deviceId.compareTo(ueGuid) != 0) {
      throw new InvalidMessageException(ueGuid.toString());
    }

    cryptoService.verifyBytes(nonce4, getStorage().getNonce4());

    reply.set(Const.SM_MSG_ID, Const.TO1_RV_REDIRECT);
    reply.set(Const.SM_BODY, getStorage().getRedirectBlob());
    getStorage().completed(request, reply);
  }

  protected void doError(Composite request, Composite reply) {
    reply.clear();
    getStorage().failed(request, reply);
  }

  @Override
  public boolean dispatch(Composite request, Composite reply) {
    switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
      case Const.TO1_HELLO_RV:
        doHello(request, reply);
        return false;
      case Const.TO1_PROVE_TO_RV:
        doProveOwner(request, reply);
        return true;
      case Const.ERROR:
        doError(request, reply);
        return true;
      default:
        throw new RuntimeException(new UnsupportedOperationException());
    }
  }
}
