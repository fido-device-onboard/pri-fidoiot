// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * To2 Server message processing service.
 */
public abstract class To2ServerService extends MessagingService {

  public abstract To2ServerStorage getStorage();

  protected void doHello(Composite request, Composite reply) {
    getStorage().starting(request, reply);

    try {
      Composite body = request.getAsComposite(Const.SM_BODY);

      UUID guid = body.getAsUuid(Const.FIRST_KEY);
      byte[] nonceTo2ProveOv = body.getAsBytes(Const.SECOND_KEY);
      String kexName = body.getAsString(Const.THIRD_KEY);
      String cipherName = body.getAsString(Const.FOURTH_KEY);
      Composite sigInfoA = body.getAsComposite(Const.FIFTH_KEY);

      Composite ownerState = getCryptoService()
          .getKeyExchangeMessage(kexName, Const.KEY_EXCHANGE_A, null);

      getStorage().setOwnerState(ownerState);
      getStorage().setCipherName(cipherName);
      getStorage().setGuid(guid);

      Composite voucher = getStorage().getVoucher();

      Composite payload = Composite.newArray()
          .set(Const.FIRST_KEY, voucher.getAsComposite(Const.OV_HEADER))
          .set(Const.SECOND_KEY, voucher.getAsComposite(Const.OV_ENTRIES).size())
          .set(Const.THIRD_KEY, voucher.getAsComposite(Const.OV_HMAC))
          .set(Const.FOURTH_KEY, nonceTo2ProveOv)
          .set(Const.FIFTH_KEY, getCryptoService().getSigInfoB(sigInfoA))
          .set(Const.SIXTH_KEY, ownerState.getAsBytes(Const.FIRST_KEY));

      byte[] nonceTo2ProveDv = getCryptoService().getRandomBytes(Const.NONCE16_SIZE);
      getStorage().setNonceTo2ProveDv(nonceTo2ProveDv);
      getStorage().setSigInfoA(sigInfoA);
      Composite ownerKey = getCryptoService().getOwnerPublicKey(voucher);
      Composite uph = Composite.newMap()
          .set(Const.CUPH_NONCE, nonceTo2ProveDv)
          .set(Const.CUPH_PUBKEY, ownerKey);

      Composite pubEncKey = getCryptoService().getOwnerPublicKey(voucher);
      PublicKey ownerPublicKey = getCryptoService().decode(pubEncKey);
      Composite cose = null;
      try (CloseableKey key = new CloseableKey(
          getStorage().getOwnerSigningKey(ownerPublicKey))) {
        cose = getCryptoService().sign(
            key.get(), payload.toBytes(), getCryptoService().getCoseAlgorithm(ownerPublicKey));
      } catch (IOException e) {
        throw new DispatchException(e);
      }

      cose.set(Const.COSE_SIGN1_UNPROTECTED, uph);
      reply.set(Const.SM_MSG_ID, Const.TO2_PROVE_OVHDR);
      reply.set(Const.SM_BODY, cose);
      getStorage().started(request, reply);

    } catch (Exception e) {
      getStorage().failed(request, reply);
      throw e;
    }
  }

  protected void doGetOpEntry(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    try {
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

    } catch (Exception e) {
      getStorage().failed(request, reply);
      throw e;
    }
  }

  protected void doProveDevice(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    try {
      Composite body = request.getAsComposite(Const.SM_BODY);
      Composite voucher = getStorage().getVoucher();
      Composite sigInfoA = getStorage().getSigInfoA();
      PublicKey deviceKey = getCryptoService().getDevicePublicKey(voucher);

      Composite certPath = voucher.getAsComposite(Const.OV_DEV_CERT_CHAIN);

      if (!getCryptoService().verify(deviceKey,
          body,
          sigInfoA,
          getStorage().getOnDieService(),
          certPath)) {
        throw new InvalidMessageException();
      }

      Composite payload = Composite.fromObject(
          body.getAsBytes(Const.COSE_SIGN1_PAYLOAD));

      Composite iotClaim = payload.getAsComposite(Const.EAT_FDO);
      byte[] kexB = iotClaim.getAsBytes(Const.FIRST_KEY);

      Composite pubEncKey = getCryptoService().getOwnerPublicKey(voucher);
      PublicKey ownerPublicKey = getCryptoService().decode(pubEncKey);
      KeyExchangeResult kxResult = getCryptoService().getSharedSecret(kexB,
          getStorage().getOwnerState(), getStorage().getOwnerSigningKey(ownerPublicKey));

      Composite cipherState = getCryptoService().getEncryptionState(kxResult,
          getStorage().getCipherName());
      getStorage().setOwnerState(cipherState);

      Composite unp = body.getAsComposite(Const.COSE_SIGN1_UNPROTECTED);
      byte[] nonceTo2SetupDv = unp.getAsBytes(Const.EUPH_NONCE);
      getStorage().setNonceTo2SetupDv(nonceTo2SetupDv);

      payload = Composite.newArray();

      // TO-DO: revisit this approach of triggering reuse in case of null entries from storage
      Composite replacementRvInfo = getStorage().getReplacementRvInfo();
      if (replacementRvInfo == null) {
        Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
        replacementRvInfo = ovh.getAsComposite(Const.OVH_RENDEZVOUS_INFO);
      }

      UUID replacementGuid = getStorage().getReplacementGuid();
      if (replacementGuid == null) {
        Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
        replacementGuid = ovh.getAsUuid(Const.OVH_GUID);
      }
      Composite replacementKey = getStorage().getReplacementOwnerKey();
      if (replacementKey == null) {
        replacementKey = getCryptoService().getOwnerPublicKey(voucher);
      }

      payload.set(Const.FIRST_KEY, replacementRvInfo);
      payload.set(Const.SECOND_KEY, replacementGuid);
      payload.set(Const.THIRD_KEY, nonceTo2SetupDv);
      payload.set(Const.FOURTH_KEY, replacementKey);

      // TO2SetupDevicePayload is signed by Owner2, which in this code is this owner
      // probably replacing the manufacturer-owner.
      PublicKey replPubKey = getCryptoService().decode(replacementKey);
      try (CloseableKey key =
          new CloseableKey(getStorage().getOwnerSigningKey(replPubKey))) {
        payload = getCryptoService().sign(
            key.get(), payload.toBytes(), getCryptoService().getCoseAlgorithm(replPubKey));
      } catch (IOException e) {
        throw new DispatchException(e);
      }

      body = getCryptoService().encrypt(
          payload.toBytes(),
          getStorage().getOwnerState());
      reply.set(Const.SM_MSG_ID, Const.TO2_SETUP_DEVICE);
      reply.set(Const.SM_BODY, body);
      getStorage().continued(request, reply);

    } catch (Exception e) {
      getStorage().failed(request, reply);
      throw e;
    }
  }

  protected void doDeviceServiceInfoReady(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    try {
      Composite body = request.getAsComposite(Const.SM_BODY);
      Composite message = Composite.fromObject(getCryptoService().decrypt(body,
          getStorage().getOwnerState()));

      Object hmacObj = message.get(Const.FIRST_KEY);
      if (!hmacObj.equals(Optional.empty())) {
        try {
          // check if its Composite type
          Composite hmac = Composite.fromObject(hmacObj);
          getStorage().setReplacementHmac(hmac.toBytes());
        } catch (IllegalArgumentException r) {
          // check if its Cbor Null. if yes, set the Cbor null bytes as replacement hmac
          // TO-DO: throw exception if comparison yields false
          if (PrimitivesUtil.isCborNull(hmacObj)) {
            getStorage().setReplacementHmac(PrimitivesUtil.getCborNullBytes());
          }
        }
      }

      int ownerMtu = 0;
      Object maxOwnerServiceInfoSz = message.get(Const.SECOND_KEY);
      if (!maxOwnerServiceInfoSz.equals(Optional.empty())) {
        try {
          ownerMtu = message.getAsNumber(Const.SECOND_KEY).intValue();
        } catch (Exception e) {
          try {
            if (PrimitivesUtil.isCborNull(maxOwnerServiceInfoSz)) {
              ownerMtu = Const.DEFAULT_SERVICE_INFO_MTU_SIZE;
            }
          } catch (Exception exception) {
            throw new RuntimeException(new MessageBodyException(exception));
          }
        }
      }
      getStorage().setMaxOwnerServiceInfoMtuSz(ownerMtu);

      Composite payload = Composite.newArray();
      payload.set(
          Const.FIRST_KEY,
          (getStorage()
              .getMaxDeviceServiceInfoMtuSz()
              .equals(String.valueOf(Const.DEFAULT_SERVICE_INFO_MTU_SIZE))
              ? null
              : Integer.parseInt(getStorage().getMaxDeviceServiceInfoMtuSz())));

      body = getCryptoService().encrypt(payload.toBytes(),
          getStorage().getOwnerState());
      reply.set(Const.SM_MSG_ID, Const.TO2_OWNER_SERVICE_INFO_READY);
      reply.set(Const.SM_BODY, body);

      getStorage().prepareServiceInfo();
      getStorage().continued(request, reply);

    } catch (Exception e) {
      getStorage().failed(request, reply);
      throw e;
    }
  }

  protected void doDeviceInfo(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    try {
      Composite body = request.getAsComposite(Const.SM_BODY);
      Composite message =
          Composite.fromObject(
              getCryptoService().decrypt(body, getStorage().getOwnerState()));

      boolean isMore = message.getAsBoolean(Const.FIRST_KEY);
      Composite svi = message.getAsComposite(Const.SECOND_KEY);
      Composite sviValues = svi.size() > 0 ? svi.getAsComposite(Const.FIRST_KEY)
          : Composite.newArray();

      int numOfValues = sviValues.size();
      for (int i = 0; i < numOfValues; i++) {
        Composite sviValue = sviValues.getAsComposite(i);
        boolean moreFlag = true;
        if (i == numOfValues - 1) {
          moreFlag = isMore;
        }
        getStorage().setServiceInfo(sviValue, moreFlag);
      }

      Composite payload = getStorage().getNextServiceInfo();

      body = getCryptoService().encrypt(payload.toBytes(), getStorage().getOwnerState());
      reply.set(Const.SM_MSG_ID, Const.TO2_OWNER_SERVICE_INFO);
      reply.set(Const.SM_BODY, body);
      getStorage().continued(request, reply);

    } catch (Exception e) {
      getStorage().failed(request, reply);
      throw e;
    }
  }

  protected void doDone(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    try {
      Composite body = request.getAsComposite(Const.SM_BODY);
      Composite message = Composite.fromObject(
          getCryptoService().decrypt(body, getStorage().getOwnerState()));
      byte[] nonceTo2ProveDv = message.getAsBytes(Const.FIRST_KEY);

      getCryptoService().verifyBytes(nonceTo2ProveDv, getStorage().getNonceTo2ProveDv());

      Composite voucher = getStorage().getVoucher();
      Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
      Composite pubKey = getCryptoService().getOwnerPublicKey(voucher);
      // if reuse, do nothing.
      // create replacement ownership voucher otherwise
      if (!isReuseSelected(ovh, pubKey)) {
        // replacement info being set in the replacement voucher header
        ovh.set(Const.OVH_GUID, getStorage().getReplacementGuid());
        ovh.set(Const.OVH_RENDEZVOUS_INFO, getStorage().getReplacementRvInfo());
        ovh.set(Const.OVH_PUB_KEY, getStorage().getReplacementOwnerKey());

        // check if owner supports Resale
        // if supported, replacement hmac is set in the replacement voucher
        // if not supported, replacement hmac is discarded
        if (getStorage().getOwnerResaleSupport() && getStorage().getReplacementHmac() != null) {
          voucher.set(Const.OV_HMAC, Composite.fromObject(getStorage().getReplacementHmac()));
        } else {
          voucher.set(Const.OV_HMAC, PrimitivesUtil.getCborNullBytes());
          getStorage().discardReplacementOwnerKey();
        }
        voucher.set(Const.OV_ENTRIES, Composite.newArray());
        getStorage().storeVoucher(voucher);
      }

      Composite payload = Composite.newArray()
          .set(Const.FIRST_KEY, getStorage().getNonceTo2SetupDv());

      body = getCryptoService().encrypt(
          payload.toBytes(),
          getStorage().getOwnerState());
      reply.set(Const.SM_MSG_ID, Const.TO2_DONE2);
      reply.set(Const.SM_BODY, body);
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

  private boolean isReuseSelected(Composite ovh, Composite pubKey) {
    if (Arrays.equals(PrimitivesUtil.getCborNullBytes(), getStorage().getReplacementHmac())
        && ovh.getAsUuid(Const.OVH_GUID).equals(getStorage().getReplacementGuid())
        && (null != getStorage().getReplacementRvInfo()
        && Arrays.equals(ovh.getAsComposite(Const.OVH_RENDEZVOUS_INFO).toBytes(),
        getStorage().getReplacementRvInfo().toBytes()))
        && (null != getStorage().getReplacementOwnerKey()
        && Arrays.equals(pubKey.toBytes(), getStorage().getReplacementOwnerKey().toBytes()))) {
      return true;
    }
    return false;
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
      case Const.TO2_DEVICE_SERVICE_INFO_READY:
        doDeviceServiceInfoReady(request, reply);
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
