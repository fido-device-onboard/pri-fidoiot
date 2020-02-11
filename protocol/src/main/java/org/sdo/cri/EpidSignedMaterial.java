// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class EpidSignedMaterial {

  private static final int sverSize = 2;
  private static final int blobIdSize = 2;
  static final int fileHeaderSize = sverSize + blobIdSize;
  static final int ECDSAsigSize = 64;
  static final int rlVerSize = 4;
  //public int RLver;         // 32 bit BigEndian
  static final int n2Size = 4;
  private static final Logger mlog = LoggerFactory.getLogger(EpidSignedMaterial.class);
  /* The index into this data is the EpidFileTypes */
  private static final byte[][] epidFileTypeCode = {
      {0x00, 0x11}, {0x00, 0x0C}, {0x00, 0x0D}, {0x00, 0x0E},
      {0x00, 0x0F}, {0x00, 0x03}, {0x00, 0x0B}, {0x00, 0x13}
  };
  /* The index into this data is the EpidVersion */
  private static final byte[][] epidVersionCsmeCode = {
      {0x00, 0x01}, {0x00, 0x02}
  };
  private static final byte[][] epidVersionIoTCode = {
      {0x01, 0x00}, {0x02, 0x00}
  };
  EpidGroupingVersion epidVersion;
  private EpidFileTypes fileType;
  private EpidRlFormat format = EpidRlFormat.UNKNOWN_EPIDFORMAT;
  private boolean wasFile = false;
  public boolean wasSigned = false;
  public boolean isValid = false;
  private byte[] body = null;
  private byte[] bodyHash = null;
  private boolean versionValid = false;

  /**
   * Constructor validates a signed Epid material file for header and signature.
   *
   * @param data     - Byte array version of the Epid material to validate
   * @param version  - the EpidGroupingVerion in use
   * @param fileType - the type of file expected from EpidFileTypes
   */
  EpidSignedMaterial(
      byte[] data,
      EpidGroupingVersion version,
      EpidFileTypes fileType) throws IOException {

    //mlog.info("EpidSignedMaterial Abstract Constructor Called, size " + data.length
    //    + " version " + version.toString()
    //    + " fileType " + fileType.toString());
    this.epidVersion = version;
    this.fileType = fileType;

    if (null == data || 0 == data.length) {
      //epiddebug-mlog.info("EpidSignedMaterials created with null data");
      return;
    }

    if (data.length < fileHeaderSize + ECDSAsigSize) {
      mlog.debug("Data length was not long enough to have a file header and an ECDSA signature");
      return;
    }

    // Get the Epid version, sver in the Epid Spec
    byte[] tgtSver = new byte[sverSize];
    System.arraycopy(data, 0, tgtSver, 0, sverSize);

    // Check to see if this is the version requested
    switch (version) {
      case kEpid2x:
        if (Arrays.areEqual(epidVersionCsmeCode[EpidGroupingVersion.kEpid2x.ordinal()], tgtSver)) {
          this.format = EpidRlFormat.CSME_EPIDFORMAT;
          this.versionValid = true;
        } else if (Arrays
            .areEqual(epidVersionIoTCode[EpidGroupingVersion.kEpid2x.ordinal()], tgtSver)) {
          this.format = EpidRlFormat.IOT_EPIDFORMAT;
          this.versionValid = true;
        } else {
          // was not a valid Epidversion for the type requested
          mlog.debug("File version did not match requested version: ");
        }
        break;

      case kEpid1x:
        if (Arrays.areEqual(epidVersionCsmeCode[EpidGroupingVersion.kEpid1x.ordinal()], tgtSver)) {
          this.format = EpidRlFormat.CSME_EPIDFORMAT;
          this.versionValid = true;
        } else if (Arrays
            .areEqual(epidVersionIoTCode[EpidGroupingVersion.kEpid1x.ordinal()], tgtSver)) {
          this.format = EpidRlFormat.IOT_EPIDFORMAT;
          this.versionValid = true;
        } else {
          // was not a valid Epidversion for the type requested
          mlog.debug("File version did not match requested version");
        }
        break;

      default:
        throw new IOException("File version requested unknown");
    }

    // If the file version was not correct then it most
    // likely means that unsigned material was given rather than signed.
    // Just return and use the data as is.
    if (!this.versionValid) {
      mlog.debug("File Epid version was not correct");
      return;
    }

    // If these match, probably a file object, but still might not be

    // Get the File type, blobId in the Epid Spec
    // Validate that it is what we expect
    byte[] tgtType = new byte[blobIdSize];
    System.arraycopy(data, sverSize, tgtType, 0, blobIdSize);

    switch (fileType) {
      case kSigRlFile:
        if (Arrays.areEqual(epidFileTypeCode[EpidFileTypes.kSigRlFile.ordinal()], tgtType)) {
          this.wasFile = true;
        }
        break;

      case kPrivRlFile:
        if (Arrays.areEqual(epidFileTypeCode[EpidFileTypes.kPrivRlFile.ordinal()], tgtType)) {
          this.wasFile = true;
        }
        break;

      case kGroupRlFile:
        if (Arrays.areEqual(epidFileTypeCode[EpidFileTypes.kGroupRlFile.ordinal()], tgtType)) {
          this.wasFile = true;
        }
        break;

      case kGroupPubKeyFile:
        if (Arrays.areEqual(epidFileTypeCode[EpidFileTypes.kGroupPubKeyFile.ordinal()], tgtType)) {
          this.wasFile = true;
        }
        break;

      // We are not supporting the parsing of these file types
      case kIssuingCaPubKeyFile:
      case kPrivRlRequestFile:
      case kSigRlRequestFile:
      case kGroupRlRequestFile:
      default:
        throw new IOException("Material Not Supported");
    }

    // If this was not a file object do no more
    if (this.wasFile == false) {
      mlog.info("Not a valid file object");
      return;
    }

    // If its a file object, we will assume it is signed as well.
    // Now collect the signature verification information
    // Take a hash of the File Header plus the body
    this.body = new byte[data.length - ECDSAsigSize];
    System.arraycopy(data, 0, body, 0, (data.length - ECDSAsigSize));
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256", BouncyCastleLoader.load());
      md.update(this.body);
      this.bodyHash = md.digest();
    } catch (NoSuchAlgorithmException ex) {
      mlog.info("Not a valid algorithm");
      throw new IOException("Could not take SHA-256 hash");
    }

    // Get the ECDSA signature
    byte[] signatureEcdsa = new byte[ECDSAsigSize];
    System.arraycopy(data, data.length - ECDSAsigSize,
        signatureEcdsa, 0, ECDSAsigSize);

    this.wasSigned = true;
  }
  //public int N2;            // 32 bit BigEndian

  /**
   * Fetch a copy of the group ID properly sized.
   *
   * @param data   - the data field to read it from
   * @param offset - the offset for the field
   * @param size   - the size of the gid
   *
   * @return a byte array containing the gid
   */
  static byte[] readGid(byte[] data, int offset, int size) {
    byte[] tgtGid = new byte[size];
    System.arraycopy(data, 0, tgtGid, 0, size);
    return tgtGid;
  }

  /**
   * Get and format the Nx value.
   * This is not supported for fileType == kGroupPubKeyFile.
   *
   * @param data   - the data field to read it from
   * @param offset - the offset for the field
   *
   * @return and integer representing the value stored
   */
  static int readNval(byte[] data, int offset) {
    byte[] nx = new byte[n2Size];
    System.arraycopy(data, offset, nx, 0, n2Size);
    return EpidUtils.bytesToUint(nx).intValue();
  }

  /**
   * Get and format the RLver value.
   * This is not supported for fileType == kGroupPubKeyFile.
   *
   * @param data   - the data field to read it from
   * @param offset - the offset for the field
   *
   * @return and integer representing the value stored
   */
  static int readRlVer(byte[] data, int offset) {
    byte[] tgtRlVer = new byte[rlVerSize];
    System.arraycopy(data, offset, tgtRlVer, 0, rlVerSize);
    return EpidUtils.bytesToUint(tgtRlVer).intValue();
  }

  /**
   * Strip off the file header and EC-DSA signature from a file.
   *
   * @param data - the file data
   *
   * @return a byte array with the parts removed
   *
   * @throws IOException if the passed data is invalid
   */
  static byte[] stripFileHeaderAndSig(byte[] data) throws IOException {
    byte[] result;

    if (data == null) {
      throw new IOException("Null data block not supported");
    }

    result = new byte[data.length - (4 + 64)];
    System.arraycopy(data, 4, result, 0, (data.length - (4 + 64)));
    return result;
  }

  /**
   * Types of files to parse, from the SDK parser.
   */
  enum EpidFileTypes {
    kIssuingCaPubKeyFile,   // 00 11 - IoT Issuing CA public key file
    kGroupPubKeyFile,       // 00 0C - Group Public Key Output File Format
    kPrivRlFile,            // 00 0D - Binary Private Key Revocation List
    kSigRlFile,             // 00 0E - Binary Signature Revocation List
    kGroupRlFile,           // 00 0F - Binary Group Revocation List
    kPrivRlRequestFile,     // 00 03 - Binary Private Key Revocation Request
    kSigRlRequestFile,      // 00 0B - Binary Signature Revocation Request
    kGroupRlRequestFile,    // 00 13 - Binary Group Revocation Request
    kNumFileTypes           // Maximum number of file types
  }

  enum EpidGroupingVersion {
    kEpid1x,            // Intel(R) EPID version 1.x
    kEpid2x,            // Intel(R) EPID version 2.x
    kNumEpidVersions    // Maximum number of EPID versions
  }

  enum EpidRlFormat {
    CSME_EPIDFORMAT,    // Non IoT Processors
    IOT_EPIDFORMAT,     // IoT Processors, IoTG group
    UNKNOWN_EPIDFORMAT  // Could not determine
  }

}
