// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;

/**
 * To0 Server message processing service.
 */
public abstract class To0ServerService extends MessagingService {

  public abstract To0ServerStorage getStorage();

  protected void doHello(Composite request, Composite reply) {
    getStorage().starting(request, reply);

    request.getAsComposite(Const.SM_BODY).verifyMaxKey(Const.NO_KEYS);

    byte[] nonce = getCryptoService().getRandomBytes(Const.NONCE16_SIZE);
    getStorage().setNonceTo0Sign(nonce);
    Composite nonceTo0Sign = Composite.newArray().set(Const.FIRST_KEY, nonce);
    reply.set(Const.SM_MSG_ID, Const.TO0_HELLO_ACK);
    reply.set(Const.SM_BODY, nonceTo0Sign);
    getStorage().started(request, reply);
  }

  protected void doOwnerSign(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    //read root message
    Composite body = request.getAsComposite(Const.SM_BODY);
    body.verifyMaxKey(Const.TO0_TO1D);

    Composite to0d = body.getAsComposite(Const.TO0_TO0D);
    Composite to1d = body.getAsComposite(Const.TO0_TO1D);

    CryptoService cryptoService = getCryptoService();
    Composite to1dPayload = Composite.fromObject(to1d.getAsBytes(Const.COSE_SIGN1_PAYLOAD));
    //todo: verify TO1D payload keys and type
    Composite to1dHash = to1dPayload.getAsComposite(Const.TO1D_TO0D_HASH);
    cryptoService.verifyHash(to1dHash, to0d.toBytes());

    //reads to0d data
    Composite voucher = to0d.getAsComposite(Const.TO0D_VOUCHER);
    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    byte[] to0dNonceTo0Sign = to0d.getAsBytes(Const.TO0D_NONCETO0SIGN);
    to0d.verifyMaxKey(Const.TO0D_NONCETO0SIGN);

    //verifying voucher
    PublicKey publicKey = cryptoService.decode(ovh.getAsComposite(Const.OVH_PUB_KEY));
    if ((cryptoService.getPublicKeyType(publicKey) == Const.PK_SECP256R1)
        || (cryptoService.getPublicKeyType(publicKey) == Const.PK_SECP384R1)) {

      cryptoService.verifyVoucher(voucher);
    }

    byte[] nonceTo0Sign = getStorage().getNonceTo0Sign();
    cryptoService.verifyBytes(nonceTo0Sign, to0dNonceTo0Sign);

    Composite pubKeyEntry = getCryptoService().getOwnerPublicKey(voucher);
    PublicKey verificationKey = cryptoService.decode(pubKeyEntry);

    // Verify voucher signature
    if (!cryptoService.verify(verificationKey, to1d, null, null, null)) {
      throw new InvalidOwnerSignBodyException();
    }

    long requestedWait = to0d.getAsNumber(Const.TO0D_WAIT_SECONDS).longValue();

    long responseWait = getStorage().storeRedirectBlob(voucher, requestedWait, to1d.toBytes());
    Composite wsReply = Composite.newArray().set(Const.FIRST_KEY, responseWait);
    reply.set(Const.SM_MSG_ID, Const.TO0_ACCEPT_OWNER);
    reply.set(Const.SM_BODY, wsReply);
    getStorage().completed(request, reply);
  }

  protected void doError(Composite request, Composite reply) {
    reply.clear();
    getStorage().failed(request, reply);
  }

  @Override
  public boolean dispatch(Composite request, Composite reply) {
    switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
      case Const.TO0_HELLO:
        doHello(request, reply);
        return false;
      case Const.TO0_OWNER_SIGN:
        doOwnerSign(request, reply);
        return true;
      case Const.ERROR:
        doError(request, reply);
        return true;
      default:
        throw new RuntimeException(new UnsupportedOperationException());
    }
  }

}
