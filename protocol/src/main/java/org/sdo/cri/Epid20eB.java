// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0


package org.sdo.cri;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles the eB calculations for Epid version 2.0
 * The main rule is that the SigRl MUST be either signed or ignored.
 * SigRls with 0 entries are represented as zero length sigRls.
 */
class Epid20eB {

  private static final Logger mlog = LoggerFactory.getLogger(Epid20eB.class);
  private final EpidOnlineMaterial epidOnlineMaterial;
  private byte[] gid = null;

  /**
   * Constructor.
   *
   * @param gid - the group ID of the eB requested
   */
  public Epid20eB(byte[] gid, EpidOnlineMaterial epidOnlineMaterial) throws IOException {
    this.epidOnlineMaterial = epidOnlineMaterial;
    if (gid == null) {
      mlog.error("Group ID was null");
      throw new IOException("Group ID was null");
    }
    if (gid.length != 16) {
      mlog.error("Group ID was incorrect length");
      throw new IOException("Group ID was incorrect length");
    }
    this.gid = gid;
    //mlog.info("Epid20eB Constructor grpId_ : 0x" + DatatypeConverter.printHexBinary(gid));
  }

  /**
   * Assemble the parts into a byte array representation.
   *
   * @return a byte array containing the assembled eB data
   */
  public byte[] getArray() throws InterruptedException, IOException, TimeoutException {
    //mlog.info("Creating eB for 2.0 for GID : " + DatatypeConverter.printHexBinary(gid));

    // Load the values based on this grpId_
    EpidGroupPublicKey pubKey;
    EpidSigRl sigRl;
    byte[] sigRlBytes;
    byte[] pubKeyBytes;
    try {
      // Look online for the material
      sigRlBytes = epidOnlineMaterial.readEpidRestService(gid, EpidLib.EpidVersion.EPID_2_0,
          EpidLib.MaterialId.SIGRL);
      sigRl = new EpidSigRl(this.gid, sigRlBytes, EpidSignedMaterial.EpidGroupingVersion.kEpid2x);
      pubKeyBytes = epidOnlineMaterial.readEpidRestService(gid, EpidLib.EpidVersion.EPID_2_0,
          EpidLib.MaterialId.PUBKEY);
      pubKey = new EpidGroupPublicKey(this.gid, pubKeyBytes,
          EpidSignedMaterial.EpidGroupingVersion.kEpid2x);
    } catch (IOException ex) {
      mlog.error("Trouble getting online material");
      throw new IOException("Trouble getting online material");
    }

    //mlog.info("sigRl read " + sigRl.data.length + " : 0x"
    //    + DatatypeConverter.printHexBinary(sigRl.data));
    //mlog.info("pubKey read " + pubKey.data.length + " 0x"
    //    + DatatypeConverter.printHexBinary(pubKey.data));

    if (sigRl == null || pubKey == null) {
      mlog.error("Could not make required classes");
      throw new IOException("Could not make required classes");
    }

    // Now build the byte array
    // Apply the rules
    boolean hasSigRl = sigRl.isValid;
    boolean hasPubKey = pubKey.isValid;

    int sigRlLength = sigRl.parsedData == null ? 0 : sigRl.parsedData.length;
    int pubKeyLength = pubKey.parsedData == null ? 0 : pubKey.parsedData.length;

    // Allocate the size required
    int ebSize = 4
        + (hasSigRl ? sigRlLength : 0)
        + (hasPubKey ? pubKeyLength : 0);
    byte[] eb = new byte[ebSize];

    byte[] emptySize = {0x00, 0x00};
    byte[] size;
    int cursor = 0;

    // Copy the required data
    if (hasSigRl) {
      size = EpidUtils.shortToBytes((short) sigRlLength);
      System.arraycopy(size, 0, eb, cursor, 2);
      cursor += 2;
      System.arraycopy(sigRl.parsedData, 0, eb, cursor, sigRlLength);
      cursor += sigRlLength;
    } else {
      System.arraycopy(emptySize, 0, eb, cursor, 2);
      cursor += 2;
    }
    if (hasPubKey) {
      size = EpidUtils.shortToBytes((short) pubKeyLength);
      System.arraycopy(size, 0, eb, cursor, 2);
      cursor += 2;
      System.arraycopy(pubKey.parsedData, 0, eb, cursor, pubKeyLength);
    } else {
      System.arraycopy(emptySize, 0, eb, cursor, 2);
    }
    //mlog.info("2.0 eB " + eb.length + " bytes 0x" + DatatypeConverter.printHexBinary(eb));
    return eb;
  }
}
