// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0


package org.sdo.cri;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main library interface for the JNI interface.
 */
class EpidLib implements AutoCloseable {

  private static final Logger mlog = LoggerFactory.getLogger(EpidLib.class);
  private final int uuidLength = 16;
  private final EpidOnlineMaterial epidOnlineMaterial;
  private final EpidOnlineVerifier epidOnlineVerifier;

  /**
   * Constructor.
   */
  public EpidLib(EpidOnlineMaterial epidOnlineMaterial, EpidOnlineVerifier epidOnlineVerifier) {
    this.epidOnlineMaterial = epidOnlineMaterial;
    this.epidOnlineVerifier = epidOnlineVerifier;
  }

  /**
   * When you are done with Epid, shut it down.
   */
  public void close() {
    return;
  }

  /**
   * Prefixes data to the message to match the prefix added by the signing function in the TA.
   *
   * @param originalMessageBytes - this must not be null
   * @param offset               - must not exceed the size ofmessageBytes
   * @param length               - must actually match the length of messageBytes
   * @param nonce                - whatever size you like, but MUST not be null
   * @param taId                 - whatever size you like, but MUST not be null
   * @return a byte array holding the adapted message
   */
  private byte[] constructMessage_1_0(byte[] originalMessageBytes, int offset, int length,
      byte[] nonce, byte[] taId) {

    byte[] taIdLength = new byte[1];
    taIdLength[0] = (byte) taId.length;

    byte[] adaptedMessage = new byte[taIdLength.length + taId.length + nonce.length
        + originalMessageBytes.length];
    System.arraycopy(taIdLength, 0, adaptedMessage, 0, taIdLength.length);
    System.arraycopy(taId, 0, adaptedMessage, taIdLength.length, taId.length);
    System.arraycopy(nonce, 0, adaptedMessage, taIdLength.length + taId.length,
        nonce.length);
    System.arraycopy(originalMessageBytes, offset, adaptedMessage,
        nonce.length + taIdLength.length + taId.length, length);
    return adaptedMessage;
  }

  /**
   * Prefixes data to the message to match the prefix added by the signing function in the TA.
   *
   * @param originalMessageBytes - this must not be null
   * @param offset               - must not exceed the size ofmessageBytes
   * @param length               - must actually match the length of messageBytes
   * @param nonce                - whatever size you like, but MUST not be null
   * @param taId                 - whatever size you like, but MUST not be null
   * @return a byte array holding the adapted message
   */
  private byte[] constructMessage_1_1(byte[] originalMessageBytes, int offset, int length,
      byte[] nonce, byte[] taId) {
    byte[] prefix = new byte[48];
    prefix[4] = 0x48;
    prefix[8] = 0x08;
    byte[] zeros = new byte[uuidLength];

    byte[] adaptedMessage = new byte[prefix.length + taId.length + zeros.length + nonce.length
        + (32 - nonce.length) + originalMessageBytes.length];
    System.arraycopy(prefix, 0, adaptedMessage, 0, prefix.length);
    System.arraycopy(taId, 0, adaptedMessage, prefix.length, taId.length);
    System.arraycopy(zeros, 0, adaptedMessage, prefix.length + taId.length, zeros.length);
    System.arraycopy(nonce, 0, adaptedMessage, prefix.length + taId.length + zeros.length,
        nonce.length);
    byte[] noncePad = new byte[32 - nonce.length];
    System.arraycopy(noncePad, 0, adaptedMessage,
        prefix.length + taId.length + zeros.length + nonce.length, noncePad.length);

    System.arraycopy(originalMessageBytes, offset, adaptedMessage,
        prefix.length + taId.length + zeros.length + nonce.length + noncePad.length, length);
    return adaptedMessage;
  }

  /**
   * Get the EpidInfo eB value.
   *
   * @param groupId - pointer to a byte array holding the group ID
   * @return a byte array representation of the eB value
   */
  public byte[] getEpidInfo11_eB(byte[] groupId)
      throws InterruptedException, IOException, TimeoutException {
    if (groupId.length != 4) {
      mlog.error("groupId was not 32 bits as required");
      throw new IOException();
    }

    Epid11eB eb = new Epid11eB(groupId, epidOnlineMaterial);
    return eb.getArray();
  }

  /**
   * Get the EpidInfo eB value.
   *
   * @param groupId - pointer to a byte array holding the group ID
   * @return a byte array representation of the eB value
   */
  public byte[] getEpidInfo20_eB(byte[] groupId)
      throws InterruptedException, IOException, TimeoutException {
    if (groupId.length != EpidConstants.EPID2X_GID_SIZE) {
      mlog.error("invalid groupId length: "
          + groupId.length
          + ", expected: "
          + EpidConstants.EPID2X_GID_SIZE);
      throw new IOException();
    }
    Epid20eB eb;
    try {
      eb = new Epid20eB(groupId, epidOnlineMaterial);
      return eb.getArray();
    } catch (IOException ex) {
      mlog.error("IOException thrown making Epid20eB : " + ex.getMessage());
      return null;
    }
  }


  /* Now the eA & eB functions */

  /**
   * Verifies Epid 1.0 signature.
   * Only support verification of epid 1.0 signatures
   *
   * @param epidGroupNo epidGroupNo is a 32 bit value
   * @param signature   - the signature to check
   * @param message     - the messageBytes that was signed
   * @param nonce       - pointer to a byte array holding the nonce
   * @param taId        - pointer to a byte array holding the AppId
   * @return status
   */
  public int verify10Signature(byte[] epidGroupNo, byte[] signature, byte[] message,
      byte[] nonce, byte[] taId) throws IOException, InterruptedException {
    //epiddebug-mlog.info("TP: verify 10 signature");
    return verify1xSignature(EpidVersion.EPID_1_0, epidGroupNo, signature, message,
        nonce, taId);
  }

  /**
   * Verifies Epid 1.1 signature.
   * Only support verification of epid 1.1 signatures
   *
   * @param epidGroupNo epidGroupNo is a 32 bit value
   * @param signature   - the signature to check
   * @param message     - the messageBytes that was signed
   * @param nonce       - pointer to a byte array holding the nonce
   * @param taId        - pointer to a byte array holding the AppId
   * @return status
   */
  public int verify11Signature(byte[] epidGroupNo, byte[] signature, byte[] message,
      byte[] nonce, byte[] taId) throws IOException, InterruptedException {

    return verify1xSignature(EpidVersion.EPID_1_1, epidGroupNo, signature, message,
        nonce, taId);
  }

  //------------------------------------------------------------------
  /* Epid 2.0 Routines */

  /**
   * Verifies Epid 1.x signature.
   *
   * @param version   - the Epid version 90=1.0, 91=1.1
   * @param gid       - pointer to a 32 bit value in a byte array
   * @param signature - the signature to check
   * @param message   - the messageBytes that was signed
   * @return status
   */
  private int verify1xSignature(EpidVersion version, byte[] gid, byte[] signature,
      byte[] message, byte[] nonce, byte[] taId) throws IOException, InterruptedException {
    if (gid == null || signature == null || message == null) {
      //mlog.info("A required parameter was null, " + gid + " "
      //    + signature + " " + message);
      return EpidStatus.kEpidBadArgErr.getValue();
    }

    if (gid.length != EpidConstants.EPID1X_GID_SIZE) {
      mlog.error("Invalid Epid 1.x gid length");
      return EpidStatus.kEpidBadArgErr.getValue();
    }

    return verify1xSignatureOnline(version, gid, signature, message, nonce, taId);

  }

  /**
   * Verifies Epid 1.x signature using the online verifier.
   *
   * @param version     - the Epid version 90=1.0, 91=1.1
   * @param epidGroupNo - pointer to a 32 bit value in a byte array
   * @param signature   - the signature to check
   * @param message     - the messageBytes that was signed
   * @return status
   */
  private int verify1xSignatureOnline(EpidVersion version, byte[] epidGroupNo, byte[] signature,
      byte[] message, byte[] nonce, byte[] taId) throws IOException, InterruptedException {
    //epiddebug-int grpNo = Epid11eB.bytesToUint(epidGroupNo);
    //epiddebug-mlog.info("verify1xSignatureOnline: group number {}", grpNo);

    byte[] adaptedMessage;
    if (version == EpidVersion.EPID_1_1) {
      adaptedMessage = constructMessage_1_1(message, 0, message.length, nonce, taId);
    } else {
      adaptedMessage = constructMessage_1_0(message, 0, message.length, nonce, taId);
    }

    return epidOnlineVerifier.verifyOnline(
        version,
        epidGroupNo,
        adaptedMessage,
        signature,
        this);
  }

  //------------------------------------------------------------------
  /* Epid 1.0 & 1.1 Routines */

  /**
   * Verifies Epid 2.0 signature using online verification.
   * Only support verification of epid 2.0 signatures
   *
   * @param gid       - pointer to a 128 bit byte array
   * @param hashAlg   - An enum of the hash alg
   * @param signature - pointer to the byte array of the signature signature
   * @param msg       - pointer to a byte array of the messageBytes
   * @return status
   */
  public int verify20Signature(byte[] gid, int hashAlg, byte[] signature,
      byte[] msg) throws IOException, InterruptedException {
    if (gid == null || gid.length != 128 / 8) {
      mlog.error("gid was not correct");
      return EpidStatus.kEpidBadArgErr.getValue();
    }

    if (hashAlg != HashAlg.KSHA256.getValue() && hashAlg != HashAlg.KSHA512.getValue()) {
      mlog.error("HashAlg was not correct");
      return EpidStatus.kEpidBadArgErr.getValue();
    }

    if (msg == null) {
      mlog.error("msg was null");
      return EpidStatus.kEpidBadArgErr.getValue();
    }

    return verify20SignatureOnline(gid, hashAlg, signature, msg);
  }

  /**
   * Verifies Epid 2.0 signature using online verification.
   * Only support verification of epid 2.0 signatures
   *
   * @param epidGroupNo - pointer to a 128 bit byte array
   * @param hashAlg     - An enum of the hash alg
   * @param signature   - pointer to the byte array of the signature signature
   * @param msg         - pointer to a byte array of the messageBytes
   * @return status
   */
  private int verify20SignatureOnline(byte[] epidGroupNo, int hashAlg, byte[] signature,
      byte[] msg) throws IOException, InterruptedException {
    if (epidGroupNo == null || epidGroupNo.length != 128 / 8) {
      return EpidStatus.kEpidBadArgErr.getValue();
    }

    if (hashAlg != HashAlg.KSHA256.getValue() && hashAlg != HashAlg.KSHA512.getValue()) {
      return EpidStatus.kEpidBadArgErr.getValue();
    }

    if (signature == null || msg == null) {
      return EpidStatus.kEpidBadArgErr.getValue();
    }

    return epidOnlineVerifier.verifyOnline(
        EpidVersion.EPID_2_0, epidGroupNo, msg, signature, this);
  }

  enum EpidStatus {
    kEpidNoErr(0),                    //!< no error
    kEpidErr(-999),                   //!< unspecified error
    kEpidBadArgErr(-997);

    private final int value;

    EpidStatus(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }


  enum EpidVersion {
    EPID_1_0,
    EPID_1_1,
    EPID_2_0
  }


  enum HashAlg {
    KSHA256(0),
    KSHA384(1),
    KSHA512(2);

    private final int value;

    HashAlg(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  enum MaterialId {
    SIGRL,
    PRIVRL,
    PUBKEY,
    PUBKEY_CRT_BIN,
    PUBKEY_CRT
  }

}
