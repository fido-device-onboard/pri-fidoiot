// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0


package org.sdo.cri;

import java.io.IOException;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EpidGroupPublicKey extends EpidSignedMaterial {

  private static final Logger mlog = LoggerFactory.getLogger(EpidGroupPublicKey.class);
  private static final int h11xSize = 96;
  private static final int h21xSize = 96;
  private static final int h1Size = 64;
  private static final int h2Size = 64;
  private static final int wSize = 128;
  private static final int body1xSize = h11xSize + h21xSize + wSize;
  private static final int bodySize = h1Size + h2Size + wSize;
  private static final int header1xSize = EpidConstants.EPID1X_GID_SIZE + body1xSize;
  private static final int header2xSize = EpidConstants.EPID2X_GID_SIZE + bodySize;
  private byte[] data;
  public byte[] parsedData = null;
  private byte[] gid;     // 32 or 128 bit BigEndian

  /**
   * Constructor.
   */
  public EpidGroupPublicKey(byte[] gid, byte[] data, EpidGroupingVersion version)
      throws IOException {
    super(data, version, EpidFileTypes.kGroupPubKeyFile);
    // When constructing, try to parse and then throw exception if not valid
    // Save passed values
    //mlog.info("----------------- Starting EpidGroupPublicKey ---------------------------");

    // The super did the signed data parsing, now we should have the
    // unsigned data so do the parsing on it.
    this.data = data;
    this.epidVersion = version;
    this.gid = gid;
    if (data == null || gid == null) {
      mlog.error("EpidGroupPublicKey invalid data");
      throw new IOException("Invalid params");
    }

    //mlog.info("Constructing EpisGroupPublicKey from " + this.data.length 
    //    + " bytes : 0x" + DatatypeConverter.printHexBinary(this.data));
    //mlog.info("Constructing with gid : 0x" + DatatypeConverter.printHexBinary(this.gid));
    //mlog.info("Constructing with EpidVersion : " + this.epidVersion.toString());

    // Check based on reported version
    switch (version) {
      case kEpid2x:
        this.epidVersion = EpidGroupingVersion.kEpid2x;
        //mlog.info("Parsing 2.x GrpPubKey");

        // Determine if this is a signed file and requires stripping
        if (data.length == (fileHeaderSize + header2xSize + ECDSAsigSize)) {
          // Size is correct for a signed GrpPubKey
          //mlog.info("Appears to be signed kEpid2xIoT file");

          if (this.wasSigned) {
            // Now we know the size, make the parsed value
            this.parsedData = stripFileHeaderAndSig(this.data);

            // Now check for validity, if fails, throws IOException
            parseUnpacked2x(gid, parsedData);
            this.isValid = true;

          } else {
            // Was not a GrpPubKey file type
            mlog.error("Not a valid GrpPubKey file");
            throw new IOException("Not a valid GrpPubKey file");
          }
        } else {

          // Does not match classic Signed 2.0 size
          // Maybe it is already stripped, use as is if valid
          //mlog.info("Appears to be unsigned kEpid2x file");
          this.wasSigned = false;
          if (data.length < header2xSize) {
            this.isValid = false;
            mlog.error("GrpPubKey data was not valid, not signed, and too small to be unsigned");
            break;
          }

          parsedData = data;
          parseUnpacked2x(gid, parsedData);
          // if not verified will throw IOException, Data verified
          this.isValid = true;

        }
        break;

      case kEpid1x:
        // Was Epid 1.x, is it a valid 1.x signed SigRl?
        if (data.length == fileHeaderSize + header1xSize + ECDSAsigSize) {
          //mlog.info("Parsing signed 1.x GrpPubKey");

          if (this.wasSigned) {
            // Now we know the size, make the return value
            this.parsedData = stripFileHeaderAndSig(this.data);
          }

          parseUnpacked1x(this.gid, this.parsedData);
          this.isValid = true;

        } else {
          // Maybe it is already stripped, use as is if valid
          //mlog.info("Appears to be unsigned kEpid1x file of length " + this.data.length);
          this.wasSigned = false;

          if (data.length == fileHeaderSize + header1xSize + ECDSAsigSize) {
            // It had a file header
            //mlog.info("Appears to be valid with file header of length " + this.data.length);
            this.parsedData = stripFileHeaderAndSig(this.data);
            this.parsedData = new byte[this.data.length - fileHeaderSize];
            System.arraycopy(
                this.data, fileHeaderSize,
                this.parsedData, 0,
                this.data.length - fileHeaderSize);
          } else if (this.data.length == header1xSize) {
            // Valid unsigned no file header
            //mlog.info("Appears to be valid no file header of length " + this.data.length);
            this.parsedData = this.data;
          } else {
            mlog.error("Appears to be junk of length " + this.data.length);
            throw new IOException("Invalid data");
          }
          //mlog.info("parsedData : 0x" + DatatypeConverter.printHexBinary(this.parsedData));

          parseUnpacked1x(this.gid, this.parsedData);
          // if not verified will throw IOException, Data verified
          this.isValid = true;
        }
        break;

      default:
        mlog.error("Invalid version for EpidVersion passed to constructor");
    }
    //mlog.info("EpidGroupPublicKey result : " + this.toString());
    //mlog.info("--------------- Ending EpidSigRl -----------------------------");
  }

  /**
   * Provide a toString method to allow the displaying of a SigRl.
   *
   * @return A String object containing the SigRl data
   */
  @Override
  public String toString() {
    if (this.isValid) {
      return String.format(
          "\n======================================================="
              + "\nGrpPubKey was Valid for " + this.epidVersion.toString()
              + ", GID : 0x" + Hex.toHexString(this.gid)
              + "\nWas Signed : " + this.wasSigned
              + "\nSource File size : " + this.data.length
              + "\nParsed File size : " + this.parsedData.length
              //+ "\nSource File : 0x" + DatatypeConverter.printHexBinary(this.data)
              //+ "\nParsed File : 0x" + DatatypeConverter.printHexBinary(this.parsedData)
              + "\n=======================================================");
    } else {
      return String.format(
          "\n======================================================="
              + "\nGrpPubKey was Not Valid for " + this.epidVersion.toString()
              + ", GID : 0x" + Hex.toHexString(this.gid)
              + "\nSource File size : " + this.data.length
              + "\nSource File : 0x" + Hex.toHexString(this.data)
              + "\n=======================================================");
    }
  }

  private void parseUnpacked1x(byte[] gid, byte[] data) throws IOException {
    // First check the size to make sure we don't overrun
    if (data == null || gid == null) {
      mlog.error("parseUnpacked1x : Null parameter passed");
      throw new IOException("parseUnpacked1x : Null parameter");
    }
    if (gid.length != EpidConstants.EPID1X_GID_SIZE) {
      mlog.error("parseUnpacked1x : Invalid sized gid was passed");
      throw new IOException("parseUnpacked1x : GID incorrect length");
    }

    //mlog.info("parseUnpacked1x : param gid : 0x" + DatatypeConverter.printHexBinary(gid));
    //mlog.info("parseUnpacked1x : param data " + data.length
    //    + " bytes : 0x" + DatatypeConverter.printHexBinary(data));

    if (data.length < header1xSize) {
      mlog.error("parseUnpacked1x : invalid data size");
      throw new IOException("parseUnpacked1x : Data too small");
    }

    // Get and compare the GrpPubKey's gid
    this.gid = readGid(data, 0, EpidConstants.EPID1X_GID_SIZE);
    if (!Arrays.areEqual(gid, this.gid)) {
      mlog.error("parseUnpacked1x : GID not correct");
      throw new IOException("parseUnpacked1x : GID not correct");
    }

    //mlog.info("parseUnpacked1x : valid");
  }

  private void parseUnpacked2x(byte[] gid, byte[] data) throws IOException {
    // First check the size to make sure we don't overrun
    if (data == null || gid == null) {
      throw new IOException("parseUnpacked2x : Null parameter");
    }

    if (data.length < header2xSize) {
      throw new IOException("parseUnpacked2x : Data too small");
    }

    // Get and compare the GrpPubKey's gid
    this.gid = readGid(data, 0, EpidConstants.EPID2X_GID_SIZE);
    if (!Arrays.areEqual(gid, this.gid)) {
      mlog.error("GrpPubKey not valid");
      throw new IOException("GrpPubKey not valid");
    }
  }
}
