// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.IOException;

/**
 * To1 Client message processing service.
 */
public abstract class To1ClientService extends DeviceService {

  protected abstract To1ClientStorage getStorage();

  protected void doHelloAck(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);

    byte[] nonce4 = body.getAsBytes(Const.FIRST_KEY);
    body.verifyMaxKey(Const.SECOND_KEY);

    byte[] ueid = getCryptoService().getUeidFromGuid(
        getStorage().getDeviceCredentials().getAsBytes(Const.DC_GUID));

    //build EAT token based on private key and sign
    Composite payload = Composite.newMap()
        .set(Const.EAT_NONCE, nonce4)
        .set(Const.EAT_UEID, ueid);

    Composite signature = null;
    try (CloseableKey key = new CloseableKey(getStorage().getSigningKey())) {
      signature = getCryptoService().sign(key.get(), payload.toBytes());
    } catch (IOException e) {
      throw new DispatchException(e);
    }

    Composite uph = Composite.newMap();
    byte[] maroePrefix = getStorage().getMaroePrefix();
    if (maroePrefix != null) {
      uph.set(Const.EAT_MAROE_PREFIX, maroePrefix);
    }

    signature.set(Const.COSE_SIGN1_UNPROTECTED, uph);

    reply.set(Const.SM_MSG_ID, Const.TO1_PROVE_TO_RV);
    reply.set(Const.SM_BODY, signature);
    getStorage().continued(request, reply);
  }

  protected void doRedirect(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    getStorage().storeSignedBlob(request.getAsComposite(Const.SM_BODY));
    reply.clear();
    getStorage().completed(request, reply);
  }

  @Override
  public boolean dispatch(Composite request, Composite reply) {
    switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
      case Const.TO1_HELLO_RV_ACK:
        doHelloAck(request, reply);
        return false;
      case Const.TO1_RV_REDIRECT:
        doRedirect(request, reply);
        return true;
      case Const.ERROR:
        doError(request, reply);
        return true;
      default:
        throw new RuntimeException(new UnsupportedOperationException());
    }
  }

  protected void doError(Composite request, Composite reply) {
    reply.clear();
    getStorage().failed(request, reply);
  }

  @Override
  public DispatchResult getHelloMessage() {

    Composite body = Composite.newArray()
        .set(Const.FIRST_KEY,
            getStorage().getDeviceCredentials().getAsBytes(Const.DC_GUID))
        .set(Const.SECOND_KEY,
            getStorage().getSigInfoA());

    getStorage().starting(Const.EMPTY_MESSAGE, Const.EMPTY_MESSAGE);
    DispatchResult dr = new DispatchResult(Composite.newArray()
        .set(Const.SM_LENGTH, Const.DEFAULT)
        .set(Const.SM_MSG_ID, Const.TO1_HELLO_RV)
        .set(Const.SM_PROTOCOL_VERSION,
            getStorage().getDeviceCredentials().getAsNumber(Const.DC_PROTVER))
        .set(Const.SM_PROTOCOL_INFO, Composite.fromObject(Const.EMPTY_BYTE))
        .set(Const.SM_BODY, body), false);

    getStorage().started(Const.EMPTY_MESSAGE, dr.getReply());
    return dr;

  }

}
