// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PublicKey;

/**
 * A Device Initialization client Service.
 */
public abstract class DiClientService extends DeviceService {

  protected abstract DiClientStorage getStorage();

  protected void doSetCredentials(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    Composite body = request.getAsComposite(Const.SM_BODY);
    Composite ovh = body.getAsComposite(Const.FIRST_KEY);

    byte[] secret = getStorage().getDeviceCredentials().getAsBytes(Const.DC_HMAC_SECRET);
    CryptoService crypto = getCryptoService();
    Composite pubKey = ovh.getAsComposite(Const.OVH_PUB_KEY);
    PublicKey mfgPubKey = crypto.decode(pubKey);

    getStorage().getDeviceCredentials().set(Const.DC_PROTVER,
        ovh.getAsNumber(Const.OVH_VERSION));
    getStorage().getDeviceCredentials().set(Const.DC_DEVICE_INFO,
        ovh.getAsString(Const.OVH_DEVICE_INFO));
    getStorage().getDeviceCredentials().set(Const.DC_GUID,
        ovh.getAsBytes(Const.OVH_GUID));
    getStorage().getDeviceCredentials().set(Const.DC_RENDEZVOUS_INFO,
        ovh.getAsComposite(Const.OVH_RENDEZVOUS_INFO));

    int hashType = crypto.getCompatibleHmacType(mfgPubKey);
    Composite hash = crypto.hash(hashType, secret, ovh.toBytes());

    hashType = crypto.getCompatibleHashType(mfgPubKey);
    Composite pubKeyHash = crypto.hash(hashType, pubKey.toBytes());
    getStorage().getDeviceCredentials().set(Const.DC_PUBLIC_KEY_HASH,
        pubKeyHash);

    reply.set(Const.SM_MSG_ID, Const.DI_SET_HMAC);
    reply.set(Const.SM_BODY, Composite.newArray().set(Const.FIRST_KEY, hash));
    getStorage().continued(request, reply);
  }

  protected void doDone(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    request.getAsComposite(Const.SM_BODY).verifyMaxKey(Const.NO_KEYS);
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
      case Const.DI_SET_CREDENTIALS:
        doSetCredentials(request, reply);
        return false;
      case Const.DI_DONE:
        doDone(request, reply);
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
    getStorage().starting(Const.EMPTY_MESSAGE, Const.EMPTY_MESSAGE);
    DispatchResult dr = new DispatchResult(Composite.newArray()
        .set(Const.SM_LENGTH, Const.DEFAULT)
        .set(Const.SM_MSG_ID, Const.DI_APP_START)
        .set(Const.SM_PROTOCOL_VERSION,
            getStorage().getDeviceCredentials().getAsNumber(Const.DC_PROTVER))
        .set(Const.SM_PROTOCOL_INFO, Composite.fromObject(Const.EMPTY_BYTE))
        .set(Const.SM_BODY,
            Composite.newArray().set(
                Const.FIRST_KEY, getStorage()
                    .getDeviceMfgInfo())), false);

    getStorage().started(Const.EMPTY_MESSAGE, dr.getReply());
    return dr;

  }
}
