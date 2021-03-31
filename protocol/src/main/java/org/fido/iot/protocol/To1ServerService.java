// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.UUID;

/**
 * To1 Server message processing service.
 */
public abstract class To1ServerService extends MessagingService {

  public abstract To1ServerStorage getStorage();

  protected void doHello(Composite request, Composite reply) {

    getStorage().starting(request, reply);

    try {
      Composite body = request.getAsComposite(Const.SM_BODY);
      final UUID guid = body.getAsUuid(Const.FIRST_KEY);
      body.verifyMaxKey(Const.SECOND_KEY);

      getStorage().setGuid(guid);

      byte[] nonceTo1Proof = getCryptoService().getRandomBytes(Const.NONCE16_SIZE);
      getStorage().setNonceTo1Proof(nonceTo1Proof);
      Composite sigA = body.getAsComposite(Const.SECOND_KEY);
      getStorage().setSigInfoA(sigA);

      body = Composite.newArray()
          .set(Const.FIRST_KEY, nonceTo1Proof)
          .set(Const.SECOND_KEY,
              getCryptoService().getSigInfoB(sigA));

      reply.set(Const.SM_MSG_ID, Const.TO1_HELLO_RV_ACK);
      reply.set(Const.SM_BODY, body);
      getStorage().started(request, reply);

    } catch (Exception e) {
      getStorage().failed(request, reply);
      throw e;
    }
  }

  protected void doProveOwner(Composite request, Composite reply) {

    getStorage().continuing(request, reply);

    try {
      Composite body = request.getAsComposite(Const.SM_BODY);
      CryptoService cryptoService = getCryptoService();
      Composite sigA = getStorage().getSigInfoA();
      PublicKey deviceKey = null;
      if (null == sigA || !Arrays.asList(Const.SG_EPIDv10, Const.SG_EPIDv11)
          .contains(sigA.getAsNumber(Const.FIRST_KEY).intValue())) {
        deviceKey = getStorage().getVerificationKey();
      }

      // verify the data signed by the device key
      if (!cryptoService.verify(
              deviceKey,
              body,
              sigA,
              getStorage().getOnDieService(),
              null)) {
        throw new InvalidMessageException();
      }

      Composite payload = Composite.fromObject(
          body.getAsBytes(Const.COSE_SIGN1_PAYLOAD));

      byte[] nonceTo1Proof = payload.getAsBytes(Const.EAT_NONCE);
      UUID ueGuid = getCryptoService().getGuidFromUeid(
          payload.getAsBytes(Const.EAT_UEID));
      UUID deviceId = getStorage().getGuid();

      if (deviceId.compareTo(ueGuid) != 0) {
        throw new InvalidMessageException(ueGuid.toString());
      }

      cryptoService.verifyBytes(nonceTo1Proof, getStorage().getNonceTo1Proof());

      reply.set(Const.SM_MSG_ID, Const.TO1_RV_REDIRECT);
      reply.set(Const.SM_BODY, getStorage().getRedirectBlob());
      getStorage().completed(request, reply);

    } catch (Exception e) {
      getStorage().failed(request, reply);
      throw e;
    }
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
