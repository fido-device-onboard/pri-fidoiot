// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EpidSigRl extends EpidSignedMaterial {

  private static final Logger mlog = LoggerFactory.getLogger(EpidSigRl.class);
  private static final int header1xSize = EpidConstants.EPID1X_GID_SIZE + rlVerSize + n2Size;
  private static final int header2xSize = EpidConstants.EPID2X_GID_SIZE + rlVerSize + n2Size;
  private static final int bkSize = 128;
  private EpidGroupingVersion version;
  public byte[] data = null;
  public byte[] parsedData = null;
  public int n2;            // 32 bit BigEndian
  private int rlver;         // 32 bit BigEndian
  private byte[] gid = null;     // 32 or 128 bit BigEndian


  /**
   * The EpidSigRl class does not allow null or malformed data to build an invalid EpidSigRl.
   *
   * @param gid     - the group id of the SigRl we are building
   * @param data    - the data that we think is a sigRl
   * @param version - the Epid version for the sigRl
   */
  public EpidSigRl(byte[] gid, byte[] data, EpidGroupingVersion version) throws IOException {
    super(data, version, EpidFileTypes.kSigRlFile);
    // When constructing, try to parse and then throw exception if not valid
    // Save passed values
    //mlog.info("----------------- Starting EpidSigRl ---------------------------");

    if (gid == null) {
      throw new IOException("Invalid params, gid was null");
    }
    this.gid = gid;
    this.version = version;

    if (null == data || 0 == data.length) {
      // Support the production of a null content SigRl
      //mlog.info("Constructing EpidSigRl from null");
      this.data = new byte[0];
      this.parsedData = new byte[0];
      return;
    }

    this.data = data;
    //mlog.info("Constructing EpidSigRl from " + this.data.length
    //    + " bytes : 0x" + DatatypeConverter.printHexBinary(this.data));
    //mlog.info("Constructing with gid : 0x" + DatatypeConverter.printHexBinary(this.gid));
    //mlog.info("Constructing with EpidVersion : " + this.epidVersion.toString());

    // Check based on reported version
    switch (version) {
      case kEpid2x:
        //mlog.info("Parsing 2.x sigRl");

        // Determine if this is a signed file and requires stripping
        if ((data.length - (fileHeaderSize + header2xSize + ECDSAsigSize)) % bkSize == 0) {
          // Size is correct for a signed sigRl
          //mlog.info("Appears to be signed kEpid2xIoT file");

          if (this.wasSigned) {
            // This is a valid file object
            // Get the number of revocation entries
            this.n2 = readNval(data, (fileHeaderSize + EpidConstants.EPID2X_GID_SIZE + rlVerSize));

            // Now we know the size, make the parsed value
            this.parsedData = stripFileHeaderAndSig(this.data);

            // Now check for validity, if fails, throws IOException
            parseUnpacked2x(gid, this.n2, parsedData);
            this.isValid = true;

          } else {
            // Was not a SigRl file type
            mlog.error("Not a valid SigRl file");
            throw new IOException("Not a valid SigRl file");
          }
        } else {
          // Does not match classic Signed 2.0 size
          // Maybe it is already stripped, use as is if valid

          if (data.length < header2xSize) {
            this.isValid = false;
            mlog.error("SigRl data was not valid, not signed, and too small to be unsigned");
            throw new IOException("Not a valid unsigned SigRl file");
          }

          // Get the number of revocation entries
          this.n2 = readNval(data, (EpidConstants.EPID2X_GID_SIZE + rlVerSize));

          this.parsedData = data;
          parseUnpacked2x(gid, this.n2, this.parsedData);
          this.isValid = true;
        }
        break;

      case kEpid1x:
        // Was Epid 1.x, is it a valid 1.x signed SigRl?
        if ((data.length - (fileHeaderSize + header1xSize + ECDSAsigSize)) % bkSize == 0) {
          //mlog.info("Parsing signed 1.x sigRl");

          if (this.wasSigned) {
            // Get the number of revocation entries
            this.n2 = readNval(data, (fileHeaderSize + EpidConstants.EPID1X_GID_SIZE + rlVerSize));
            //mlog.info("N2 : " + this.N2);

            // Now we know the size, make the return value
            this.parsedData = stripFileHeaderAndSig(this.data);
            //mlog.info("Copied " + returnSize + " bytes to parsedData");

            parseUnpacked1x(this.gid, this.n2, this.parsedData);
            this.isValid = true;
          } else {
            throw new IOException("blobId was wrong");
          }
        } else {
          // Maybe it is already stripped, use as is if valid
          //mlog.info("Appears to be unsigned kEpid1x file of length " + this.data.length);
          if ((data.length - (fileHeaderSize + header1xSize)) % bkSize == 0) {
            // It had a file header
            //mlog.info("Appears to be valid with file header of length " + this.data.length);
            this.parsedData = new byte[this.data.length - fileHeaderSize];
            System.arraycopy(
                this.data, fileHeaderSize,
                this.parsedData, 0,
                this.data.length - fileHeaderSize);
          } else if ((this.data.length - header1xSize) % bkSize == 0) {
            // Valid unsigned no file header
            //mlog.info("Appears to be valid no file header of length " + this.data.length);
            this.parsedData = this.data;
          } else {
            mlog.error("Appears to be junk of length " + this.data.length);
            throw new IOException("Invalid data");
          }
          //mlog.info("parsedData : 0x" + DatatypeConverter.printHexBinary(this.parsedData));

          // Get the number of revocation entries
          this.n2 = readNval(data, (EpidConstants.EPID1X_GID_SIZE + rlVerSize));

          parseUnpacked1x(this.gid, this.n2, this.parsedData);
          // if not verified will throw IOException, Data verified
          this.isValid = true;
        }
        break;

      default:
        mlog.error("Invalid version for EpidVersion passed to constructor");
    }
    //epiddebug-mlog.info("EpidSigRl result : " + this.toString());
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
              + "\nSigRl was Valid for " + this.epidVersion.toString()
              + ", GID : 0x" + Hex.toHexString(this.gid)
              + "\nWas Signed : " + this.wasSigned
              + "\nRLver : " + this.rlver
              + "\nHashCode : " + this.hashCode()
              + "\nEntries : " + this.n2
              + "\nSource File size : " + this.data.length
              + "\nParsed File size : " + this.parsedData.length
              //+ "\nSource File : 0x" + DatatypeConverter.printHexBinary(this.data)
              //+ "\nParsed File : 0x" + DatatypeConverter.printHexBinary(this.parsedData)
              + "\n=======================================================");
    } else {
      return String.format(
          "\n======================================================="
              + "\nSigRl was Not Valid for " + this.epidVersion.toString()
              + ", GID : 0x" + Hex.toHexString(this.gid)
              + "\nSource File size : " + this.data.length
              + "\nSource File : 0x" + Hex.toHexString(this.data)
              + "\n=======================================================");
    }
  }

  private void parseUnpacked1x(byte[] gid, int n2, byte[] data) throws IOException {
    // First check the size to make sure we don't overrun
    if (data == null || gid == null) {
      mlog.error("parseUnpacked1x : Null parameter passed");
      throw new IOException("parseUnpacked1x : Null parameter");
    }
    if (gid.length != EpidConstants.EPID1X_GID_SIZE) {
      mlog.error("Invalid sized gid was passed");
      throw new IOException("GID incorrect length");
    }

    //mlog.info("parseUnpacked1x : param gid : 0x" + DatatypeConverter.printHexBinary(gid));
    //mlog.info("parseUnpacked1x : param n2 : " + n2);
    //mlog.info("parseUnpacked1x : param data " + data.length
    //    + " bytes : 0x" + DatatypeConverter.printHexBinary(data));

    if (data.length != (header1xSize + (n2 * bkSize))) {
      mlog.error("SigRl data was incorrect size");
      throw new IOException("SigRl data was incorrect size");
    }

    // Get and compare the SigRl's gid
    this.gid = readGid(data, 0, EpidConstants.EPID1X_GID_SIZE);
    if (!Arrays.areEqual(gid, this.gid)) {
      mlog.error("SigRl not matching GID");
      throw new IOException("SigRl not matching GID");
    }

    // Now parse the RLver value
    this.rlver = readRlVer(data, EpidConstants.EPID1X_GID_SIZE);
    //mlog.info("SigRl RLver : " + this.RLver);
  }

  private void parseUnpacked2x(byte[] gid, int n2, byte[] data) throws IOException {
    if (data == null || gid == null) {
      mlog.error("SigRl data or gid was null");
      throw new IOException("parseUnpacked2x : Null parameter");
    }

    if (gid.length != EpidConstants.EPID2X_GID_SIZE) {
      mlog.error("GID was incorrect size");
      throw new IOException("GID was incorrect size");
    }

    if (data.length != (header2xSize + (n2 * bkSize))) {
      mlog.error("SigRl data was incorrect size");
      throw new IOException("SigRl data was incorrect size");
    }

    // Get and compare the SigRl's gid
    this.gid = readGid(data, 0, EpidConstants.EPID2X_GID_SIZE);
    if (!Arrays.areEqual(gid, this.gid)) {
      mlog.error("SigRl not matching GID");
      throw new IOException("SigRl not matching GID");
    }

    // Now parse the RLver value
    this.rlver = readRlVer(data, EpidConstants.EPID2X_GID_SIZE);
    //mlog.info("SigRl RLver : " + this.RLver);
  }
}
