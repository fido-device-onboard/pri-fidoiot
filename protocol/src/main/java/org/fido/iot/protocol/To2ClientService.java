// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.UUID;

/**
 * To2 Client message processing service.
 */
public abstract class To2ClientService extends DeviceService {

  // values to verify entry
  protected int numEntries;
  protected int entryNum;
  protected Composite prevEntryHash;
  protected Composite prevEntryKey;

  protected Composite hdrHash;
  protected Composite voucherHdr;
  protected Composite cupKey;
  protected byte[] nonce5;
  protected byte[] nonce6;
  protected byte[] nonce7;
  protected byte[] kexA;
  protected Composite ownState;

  protected Enumeration<Composite> dviEnumerator;

  protected abstract To2ClientStorage getStorage();

  protected Composite getVoucherHeader() {
    return voucherHdr;
  }

  protected Composite getHeaderHash() {
    return hdrHash;
  }

  protected void verifyEntry(int entryNum, Composite entry) {

    if (entryNum != this.entryNum) {
      throw new InvalidMessageException();
    }
    PublicKey verifyKey = getCryptoService().decode(this.prevEntryKey);
    if (!getCryptoService().verify(verifyKey, entry, null)) {
      throw new InvalidMessageException();
    }

    Composite payload = Composite.fromObject(entry.getAsBytes(Const.COSE_SIGN1_PAYLOAD));
    Composite hashPrevEntry = payload.getAsComposite(Const.OVE_HASH_PREV_ENTRY);
    Composite hdrHash = payload.getAsComposite(Const.OVE_HASH_HDR_INFO);

    ByteBuffer value1 = hashPrevEntry.getAsByteBuffer(Const.HASH);
    ByteBuffer value2 = this.prevEntryHash.getAsByteBuffer(Const.HASH);
    if (value1.compareTo(value2) != 0) {
      throw new InvalidMessageException();
    }

    value1 = hdrHash.getAsByteBuffer(Const.HASH);
    value2 = this.hdrHash.getAsByteBuffer(Const.HASH);
    if (value1.compareTo(value2) != 0) {
      throw new InvalidMessageException();
    }

    //update variable
    this.prevEntryKey = payload.getAsComposite(Const.OVE_PUB_KEY);
    int hashType = getCryptoService().getCompatibleHashType(verifyKey);
    this.prevEntryHash = getCryptoService().hash(hashType, entry.toBytes());
    if (entryNum == this.numEntries - 1) {
      PublicKey key1 = getCryptoService().decode(this.cupKey);
      PublicKey key2 = getCryptoService().decode(this.prevEntryKey);
      if (getCryptoService().compare(key1, key2) != 0) {
        throw new InvalidMessageException();
      }
    }
  }

  protected void nextMessage(Composite request, Composite reply) {
    if (this.numEntries == 0 || (entryNum == (this.numEntries))) {

      PublicKey ownerKey = getCryptoService().decode(cupKey);
      Composite deviceState = getCryptoService()
          .getKeyExchangeMessage(getStorage().getKexSuiteName(), Const.KEY_EXCHANGE_B, ownerKey);

      byte[] ownSecret = getCryptoService().getSharedSecret(this.kexA, deviceState, null);

      this.ownState = getCryptoService()
          .getEncryptionState(ownSecret,
              getStorage().getCipherSuiteName());

      byte[] ueid = getCryptoService()
          .getUeidFromGuid(
              getStorage()
                  .getDeviceCredentials()
                  .getAsBytes(Const.DC_GUID));

      //build EAT token based on private key and sign
      Composite iotPayload = Composite.newArray()
          .set(Const.FIRST_KEY, deviceState.getAsBytes(Const.FIRST_KEY));

      Composite payload = Composite.newMap()
          .set(Const.EAT_NONCE, nonce6)
          .set(Const.EAT_UEID, ueid)
          .set(Const.EAT_SDO_IOT, iotPayload);

      Composite signature = null;
      try (CloseableKey key = new CloseableKey(getStorage().getSigningKey())) {
        signature = getCryptoService().sign(
            key.get(), payload.toBytes(), getCryptoService().getCoseAlgorithm(key.get()));
      } catch (IOException e) {
        throw new DispatchException(e);
      }

      nonce7 = getCryptoService().getRandomBytes(Const.NONCE16_SIZE);
      Composite uph = Composite.newMap()
          .set(Const.EUPH_NONCE, nonce7);

      byte[] maroePrefix = getStorage().getMaroePrefix();
      if (maroePrefix != null) {
        uph.set(Const.EAT_MAROE_PREFIX, maroePrefix);
      }

      signature.set(Const.COSE_SIGN1_UNPROTECTED, uph);

      reply.set(Const.SM_MSG_ID, Const.TO2_PROVE_DEVICE);
      reply.set(Const.SM_BODY, signature);

    } else {
      Composite body = Composite.newArray()
          .set(Const.FIRST_KEY, this.entryNum);
      reply.set(Const.SM_MSG_ID, Const.TO2_GET_OVNEXT_ENTRY);
      reply.set(Const.SM_BODY, body);
    }
  }

  protected void doProveHeader(Composite request, Composite reply) {
    getStorage().starting(request, reply);

    Composite cose = request.getAsComposite(Const.SM_BODY);
    Composite payload = Composite.fromObject(cose.getAsBytes(Const.COSE_SIGN1_PAYLOAD));

    nonce6 = cose.getAsComposite(Const.COSE_SIGN1_UNPROTECTED)
        .getAsBytes(Const.CUPH_NONCE);

    int entries = payload.getAsNumber(Const.SECOND_KEY).intValue();
    byte[] receivedNonce5 = payload.getAsBytes(Const.FOURTH_KEY);
    this.kexA = payload.getAsBytes(Const.SIXTH_KEY);
    this.numEntries = entries;

    //verify nonce from hello
    getCryptoService().verifyBytes(receivedNonce5, nonce5);

    Composite hmac = payload.getAsComposite(Const.THIRD_KEY);
    Composite ovh = payload.getAsComposite(Const.FIRST_KEY);
    Composite hmac1 = getCryptoService().hash(
        hmac.getAsNumber(Const.HASH_TYPE).intValue(),
        getStorage()
            .getDeviceCredentials()
            .getAsBytes(Const.DC_HMAC_SECRET),
        ovh.toBytes());

    //verify HMAC
    ByteBuffer b1 = hmac1.getAsByteBuffer(Const.HASH);
    ByteBuffer b2 = hmac.getAsByteBuffer(Const.HASH);
    if (b1.compareTo(b2) != 0) {
      throw new InvalidMessageException();
    }

    //verify this message
    Composite pub = cose.getAsComposite(Const.COSE_SIGN1_UNPROTECTED)
        .getAsComposite(Const.CUPH_PUBKEY);

    PublicKey verifyKey = getCryptoService().decode(pub);
    getCryptoService().verify(verifyKey, cose, null);

    //TODO: verify to1d

    //calculate and set hdrHash
    byte[] guid = ovh.getAsBytes(Const.OVH_GUID);
    byte[] devInfo = ovh.getAsBytes(Const.OVH_DEVICE_INFO);
    ByteBuffer buffer = ByteBuffer.allocate(guid.length + devInfo.length);
    buffer.put(guid);
    buffer.put(devInfo);
    buffer.flip();

    int hashType = getCryptoService().getCompatibleHashType(verifyKey);
    this.hdrHash = getCryptoService().hash(hashType, Composite.unwrap(buffer));

    //calculate prevHash
    byte[] ovhBytes = ovh.toBytes();
    byte[] hmacBytes = hmac.toBytes();
    buffer = ByteBuffer.allocate(ovhBytes.length + hmacBytes.length);
    buffer.put(ovhBytes);
    buffer.put(hmacBytes);
    buffer.flip();
    Composite prevHash = getCryptoService().hash(hashType, Composite.unwrap(buffer));

    Composite ovhKey = ovh.getAsComposite(Const.OVH_PUB_KEY);

    this.entryNum = 0;
    this.prevEntryHash = prevHash;
    this.prevEntryKey = ovhKey;
    this.cupKey = pub;
    this.voucherHdr = ovh;

    nextMessage(request, reply);
    getStorage().started(request, reply);
  }

  protected void doNextOpEntry(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    Composite body = request.getAsComposite(Const.SM_BODY);
    int entryNum = body.getAsNumber(Const.FIRST_KEY).intValue();
    Composite entry = body.getAsComposite(Const.SECOND_KEY);

    verifyEntry(entryNum, entry);

    this.entryNum++;
    nextMessage(request, reply);
    getStorage().continued(request, reply);
  }

  protected void doSetupDevice(Composite request, Composite reply) {
    getStorage().continuing(request, reply);

    Composite body = request.getAsComposite(Const.SM_BODY);
    Composite message = Composite.fromObject(getCryptoService().decrypt(body, this.ownState));
    byte[] receivedNonce7 = message.getAsBytes(Const.THIRD_KEY);
    getCryptoService().verifyBytes(receivedNonce7, nonce7);

    Composite ownerKey2 = message.getAsComposite(Const.FOURTH_KEY);
    Composite oldCreds = getStorage().getDeviceCredentials();
    Composite newCreds = Composite.newArray()

        .set(Const.DC_PROTVER,
            oldCreds.getAsNumber(Const.DC_PROTVER))
        .set(Const.DC_HMAC_SECRET,
            oldCreds.getAsBytes(Const.DC_HMAC_SECRET))
        .set(Const.DC_DEVICE_INFO,
            oldCreds.getAsString(Const.DC_DEVICE_INFO))
        .set(Const.DC_GUID,
            message.getAsUuid(Const.SECOND_KEY))
        .set(Const.DC_RENDEZVOUS_INFO,
            message.getAsComposite(Const.FIRST_KEY));

    PublicKey mfgPubKey = getCryptoService().decode(ownerKey2);
    int hashType = getCryptoService().getCompatibleHashType(mfgPubKey);
    Composite pubKeyHash = getCryptoService().hash(hashType, ownerKey2.toBytes());
    getStorage().getDeviceCredentials().set(Const.DC_PUBLIC_KEY_HASH,
        pubKeyHash);

    //check for credential reuse
    boolean isReuse = true;

    //check if GUIDs equal
    UUID guid1 = oldCreds.getAsUuid((Const.DC_GUID));
    UUID guid2 = newCreds.getAsUuid(Const.DC_GUID);
    if (guid1.compareTo(guid2) != 0) {
      isReuse = false;
    }
    //check if owner key equal
    if (isReuse) {
      PublicKey publicKey1 = getCryptoService().decode(cupKey);
      PublicKey publicKey2 = getCryptoService().decode(ownerKey2);
      if (getCryptoService().compare(publicKey1, publicKey2) != 0) {
        isReuse = false;
      }
    }

    //check if rvinfo equal
    if (isReuse) {
      byte[] info1 = oldCreds.getAsComposite(Const.DC_RENDEZVOUS_INFO).toBytes();
      byte[] info2 = newCreds.getAsComposite(Const.DC_RENDEZVOUS_INFO).toBytes();
      if (Arrays.compare(info1, info2) != 0) {
        isReuse = false;
      }
    }

    byte[] secret = getStorage().getReplacementHmacSecret(newCreds, isReuse);
    Composite newHash = null;

    if (secret != null) {
      Composite headerCopy = Composite.newArray();
      headerCopy.set(Const.OVH_VERSION, voucherHdr.get(Const.OVH_VERSION));
      headerCopy.set(Const.OVH_GUID, newCreds.getAsUuid(Const.DC_GUID));
      headerCopy.set(Const.OVH_RENDEZVOUS_INFO, newCreds.getAsComposite(Const.DC_RENDEZVOUS_INFO));
      headerCopy.set(Const.OVH_DEVICE_INFO, voucherHdr.get(Const.OVH_DEVICE_INFO));
      headerCopy.set(Const.OVH_PUB_KEY, ownerKey2);
      headerCopy.set(Const.OVH_CERT_CHAIN_HASH, voucherHdr.get(Const.OVH_CERT_CHAIN_HASH));
      hashType = getCryptoService().getCompatibleHmacType(getCryptoService().decode(ownerKey2));
      newHash = getCryptoService().hash(hashType, secret, headerCopy.toBytes());
    }

    Composite payload = Composite.newArray()
        .set(Const.FIRST_KEY,
            newHash != null ? newHash : PrimitivesUtil.getCborNullBytes());

    body = getCryptoService().encrypt(payload.toBytes(), this.ownState);

    reply.set(Const.SM_MSG_ID, Const.TO2_AUTH_DONE);
    reply.set(Const.SM_BODY, body);
    getStorage().continued(request, reply);
  }

  protected void doAuthDone2(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    Composite message = Composite.fromObject(getCryptoService().decrypt(body, this.ownState));

    message.verifyMaxKey(Const.NO_KEYS);

    getStorage().prepareServiceInfo();
    Composite payload = getStorage().getNextServiceInfo();
    body = getCryptoService().encrypt(payload.toBytes(), this.ownState);
    reply.set(Const.SM_MSG_ID, Const.TO2_DEVICE_SERVICE_INFO);
    reply.set(Const.SM_BODY, body);
    getStorage().continued(request, reply);
  }

  protected void doServiceInfo(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    Composite svi = Composite.fromObject(getCryptoService().decrypt(body, this.ownState));

    boolean isMore = svi.getAsBoolean(Const.FIRST_KEY);
    boolean isDone = svi.getAsBoolean(Const.SECOND_KEY);

    Composite sviValues = svi.getAsComposite(Const.THIRD_KEY);
    Composite values = sviValues.size() > 0 ? sviValues.getAsComposite(Const.FIRST_KEY)
            : Composite.newArray();
    for (int i = 0; i < values.size(); i++) {
      Composite sviValue = values.getAsComposite(i);
      getStorage().setServiceInfo(sviValue, isMore, isDone);
    }

    Composite payload = getStorage().getNextServiceInfo();

    boolean isMore2 = payload.getAsBoolean(Const.FIRST_KEY);

    if (isDone && isMore == false && isMore2 == false) {
      //change message to done
      payload = Composite.newArray()
          .set(Const.FIRST_KEY, this.nonce6);
      reply.set(Const.SM_MSG_ID, Const.TO2_DONE);
    } else {
      reply.set(Const.SM_MSG_ID, Const.TO2_DEVICE_SERVICE_INFO);
    }

    body = getCryptoService().encrypt(payload.toBytes(), ownState);
    reply.set(Const.SM_BODY, body);
    getStorage().continued(request, reply);
  }

  protected void doDone2(Composite request, Composite reply) {
    getStorage().continuing(request, reply);
    Composite body = request.getAsComposite(Const.SM_BODY);
    Composite message = Composite.fromObject(getCryptoService().decrypt(body, this.ownState));

    getCryptoService().verifyBytes(nonce7, message.getAsBytes(Const.FIRST_KEY));

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
      case Const.TO2_PROVE_OVHDR:
        doProveHeader(request, reply);
        return false;
      case Const.TO2_OVNEXT_ENTRY:
        doNextOpEntry(request, reply);
        return false;
      case Const.TO2_SETUP_DEVICE:
        doSetupDevice(request, reply);
        return false;
      case Const.TO2_AUTH_DONE2:
        doAuthDone2(request, reply);
        return false;
      case Const.TO2_OWNER_SERVICE_INFO:
        doServiceInfo(request, reply);
        return false;
      case Const.TO2_DONE2:
        doDone2(request, reply);
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

    Composite dc = getStorage().getDeviceCredentials();
    nonce5 = getCryptoService().getRandomBytes(Const.NONCE16_SIZE);
    Composite body = Composite.newArray()
        .set(Const.FIRST_KEY,
            dc.getAsBytes(Const.DC_GUID))
        .set(Const.SECOND_KEY, nonce5)
        .set(Const.THIRD_KEY, getStorage().getKexSuiteName())
        .set(Const.FOURTH_KEY, getStorage().getCipherSuiteName())
        .set(Const.FIFTH_KEY, getStorage().getSigInfoA());

    getStorage().starting(Const.EMPTY_MESSAGE, Const.EMPTY_MESSAGE);
    DispatchResult dr = new DispatchResult(Composite.newArray()
        .set(Const.SM_LENGTH, Const.DEFAULT)
        .set(Const.SM_MSG_ID, Const.TO2_HELLO_DEVICE)
        .set(Const.SM_PROTOCOL_VERSION,
            dc.getAsNumber(Const.DC_PROTVER).intValue())
        .set(Const.SM_PROTOCOL_INFO, Composite.fromObject(Const.EMPTY_BYTE))
        .set(Const.SM_BODY, body), false);

    getStorage().started(Const.EMPTY_MESSAGE, dr.getReply());
    return dr;

  }

}
