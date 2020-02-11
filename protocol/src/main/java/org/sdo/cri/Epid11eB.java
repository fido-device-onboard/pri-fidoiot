// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0


package org.sdo.cri;

import java.io.IOException;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the eB calculations for Epid version 1.0 and 1.1.
 * The main rule is that the SigRl MUST be either signed or ignored.
 * SigRls with 0 entries are represented as zero length sigRls.
 */
class Epid11eB {

  private static final Logger mlog = LoggerFactory.getLogger(Epid11eB.class);
  private final EpidOnlineMaterial epidOnlineMaterial;
  private byte[] grpId;

  /**
   * Constructor.
   *
   * @param grpId - the group ID of the eB requested
   */
  public Epid11eB(byte[] grpId, EpidOnlineMaterial epidOnlineMaterial) {
    this.grpId = grpId;
    this.epidOnlineMaterial = epidOnlineMaterial;
  }

  /**
   * Assemble the parts into a byte array representation.
   *
   * @return a byte array containing the assembled eB data
   */
  public byte[] getArray() throws IOException, InterruptedException {
    // Get the materials as directed
    //mlog.info("============== Starting Epid11eB =====================");
    //mlog.info("Creating eB for 1.x for GID : " + DatatypeConverter.printHexBinary(grpId_));

    byte[] grpCertSigma11Online;
    grpCertSigma11Online = epidOnlineMaterial
        .readEpidRestService(grpId, EpidLib.EpidVersion.EPID_1_1,
            EpidLib.MaterialId.PUBKEY_CRT);
    if (grpCertSigma11Online != null) {
      mlog.debug("grpCertSigma11Online read : " + grpCertSigma11Online.length);
    }

    byte[] grpCertSigma10Online;
    grpCertSigma10Online = epidOnlineMaterial
        .readEpidRestService(grpId, EpidLib.EpidVersion.EPID_1_1,
            EpidLib.MaterialId.PUBKEY_CRT_BIN);
    if (grpCertSigma10Online != null) {
      mlog.debug("grpCertSigma10Online read : " + grpCertSigma10Online.length);
    }

    byte[] sigRlBaOnline;
    sigRlBaOnline = epidOnlineMaterial.readEpidRestService(grpId, EpidLib.EpidVersion.EPID_1_1,
        EpidLib.MaterialId.SIGRL);

    if (sigRlBaOnline != null) {
      mlog.debug("sigRlOnline read : " + sigRlBaOnline.length);
    } else {
      mlog.debug("sigRl not found online");
    }

    EpidSigRl epidSigRl = null;
    try {
      if (sigRlBaOnline != null && (sigRlBaOnline.length > 0)) {
        mlog.info("Making EpidSigRl from online data");
        final EpidSigRl sigRlOnline =
            new EpidSigRl(this.grpId, sigRlBaOnline, EpidSigRl.EpidGroupingVersion.kEpid1x);

        if (sigRlOnline.wasSigned && sigRlOnline.isValid) {
          epidSigRl = sigRlOnline;
          mlog.debug("sigRl is online : " + epidSigRl.data.length);
        }
      }
    } catch (IOException ex) {
      mlog.error("Trouble constructing the sigRl auto 1.x for eB");
      mlog.info("Trouble constructing sigRl, will continue with sigRl.");
    }

    // retrieve the two certs for the group
    byte[] grpCertSigma10 = null;
    if (grpCertSigma10Online != null && grpCertSigma10Online.length > 0) {
      grpCertSigma10 = grpCertSigma10Online;
      mlog.debug("grpCertSigma10 is online : " + grpCertSigma10.length);
    }

    byte[] grpCertSigma11 = null;
    if (grpCertSigma11Online != null && grpCertSigma11Online.length > 0) {
      grpCertSigma11 = grpCertSigma11Online;
      mlog.debug("grpCertSigma11 is online : " + grpCertSigma11.length);
    }

    // Calculate the size of the resulting eB
    int ebSize = 0;

    // Account for grpCertSigma10
    if (grpCertSigma10 != null) {
      ebSize += 2 + grpCertSigma10.length;
    } else {
      ebSize += 2;
    }
    // Account for grpCertSigma11
    if (grpCertSigma11 != null) {
      ebSize += 2 + grpCertSigma11.length;
    } else {
      ebSize += 2;
    }
    // Account for SigRl
    Predicate<EpidSigRl> hasSigRl = sigRl -> null != sigRl && 0 < sigRl.n2;

    if (hasSigRl.test(epidSigRl)) {
      ebSize += 2 + epidSigRl.data.length;
    } else {
      ebSize += 2;
    }
    //mlog.info("ebSize : " + ebSize);

    // Size now known, construct the byte array

    byte[] eb = new byte[ebSize];
    int cursor = 0;
    byte[] empty = {0, 0};
    byte[] size;

    // Copy the values gathered

    if (grpCertSigma10 != null) {
      size = EpidUtils.shortToBytes((short) grpCertSigma10.length);
      System.arraycopy(size, 0, eb, cursor, 2);
      cursor += 2;
      System.arraycopy(grpCertSigma10, 0, eb, cursor, grpCertSigma10.length);
      cursor += grpCertSigma10.length;
    } else {
      System.arraycopy(empty, 0, eb, cursor, 2);
      cursor += 2;
    }

    if (grpCertSigma11 != null) {
      size = EpidUtils.shortToBytes((short) grpCertSigma11.length);
      System.arraycopy(size, 0, eb, cursor, 2);
      cursor += 2;
      System.arraycopy(grpCertSigma11, 0, eb, cursor, grpCertSigma11.length);
      cursor += grpCertSigma11.length;
    } else {
      System.arraycopy(empty, 0, eb, cursor, 2);
      cursor += 2;
    }

    // Add sigRl if present
    if (hasSigRl.test(epidSigRl)) {
      size = EpidUtils.shortToBytes((short) epidSigRl.data.length);
      System.arraycopy(size, 0, eb, cursor, 2);
      cursor += 2;
      System.arraycopy(epidSigRl.data, 0, eb, cursor, epidSigRl.data.length);
    } else {
      // no sigRl so set to length of 0
      System.arraycopy(empty, 0, eb, cursor, 2);
    }
    return eb;
  }
}
