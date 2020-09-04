// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * A Device Initialization server Service.
 */
public abstract class DiServerService extends MessagingService {

  protected abstract DiServerStorage getStorage();

  protected void doAppStart(Composite request, Composite reply) {
    getStorage().starting(request, reply);
    Object createParams = request.getAsComposite(Const.SM_BODY).get(Const.FIRST_KEY);
    Composite voucher = getStorage().createVoucher(createParams);
    reply.set(Const.SM_MSG_ID, Const.DI_SET_CREDENTIALS);
    reply.set(Const.SM_BODY,
        Composite.newArray().set(
            Const.FIRST_KEY,
            voucher.get(Const.OV_HEADER)));
    getStorage().started(request, reply);
  }

  protected void setHmac(Composite request, Composite reply) {

    getStorage().continuing(request, reply);
    Composite voucher = getStorage().getVoucher();

    Composite body = request.getAsComposite(Const.SM_BODY);
    body.verifyMaxKey(Const.FIRST_KEY);

    Composite hash = body.getAsComposite(Const.FIRST_KEY);
    hash.verifyMaxKey(Const.HASH);
    voucher.set(Const.OV_HMAC, hash);
    //todo:verify voucher
    getStorage().storeVoucher(voucher);
    reply.set(Const.SM_MSG_ID, Const.DI_DONE);
    reply.set(Const.SM_BODY, Const.EMPTY_MESSAGE);
    getStorage().completed(request, reply);
  }

  protected void doError(Composite request, Composite reply) {
    reply.clear();
    getStorage().failed(request, reply);
  }

  @Override
  public boolean dispatch(Composite request, Composite reply) {
    switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
      case Const.DI_APP_START:
        doAppStart(request, reply);
        return false;
      case Const.DI_SET_HMAC:
        setHmac(request, reply);
        return true;
      case Const.ERROR:
        doError(request, reply);
        return true;
      default:
        throw new RuntimeException(new UnsupportedOperationException());
    }
  }

}
