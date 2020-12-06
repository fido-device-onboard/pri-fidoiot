// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.epid;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.InvalidMessageException;

public class EpidMaterialService {

  private byte[] getSigrl(Composite sigInfo, String epidVersion) throws IOException {
    byte[] sigrlResponse = getEpidVerificationServiceResource(sigInfo, Const.SIGRL, epidVersion);
    if (sigrlResponse == null) {
      return new byte[] {};
    }
    return sigrlResponse;
  }

  private byte[] getGroupCertSigma10(Composite sigInfo) {
    return getEpidVerificationServiceResource(
        sigInfo, Const.GROUPCERTSIGMA10,
            (Const.EPID_PROTOCOL_VERSION_V2 + Const.URL_PATH_SEPARATOR + Const.EPID_11));
  }

  private byte[] getGroupCertSigma11(Composite sigInfo) {
    return getEpidVerificationServiceResource(
        sigInfo, Const.GROUPCERTSIGMA11,
            (Const.EPID_PROTOCOL_VERSION_V2 + Const.URL_PATH_SEPARATOR + Const.EPID_11));
  }

  private byte[] getEpidVerificationServiceResource(
      Composite sigInfo, String resource, String epidVersion) {
    String path =
        String.join(Const.URL_PATH_SEPARATOR,
            Arrays.asList(
                    epidVersion,
                    Composite.toString(sigInfo.getAsBytes(1)).toUpperCase(),
                    resource.toLowerCase()));

    String url;
    try {
      url =
          new URL(EpidUtils.getEpidOnlineUrl().toString())
              .toURI()
              .resolve(Const.URL_PATH_SEPARATOR + path)
              .toString();
      return EpidHttpClient.doGet(url);
    } catch (URISyntaxException | IOException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * EPID signature from verification service.
   * @param sigA device group and epid type data
   * @return EPID eb data suitable for EPID provisioning
   * @throws IOException for unhandled IO Exceptions
   */
  public Composite getSigInfo(Composite sigA) throws IOException {

    int sgType = (int)(long) sigA.get(Const.FIRST_KEY);

    if (sigA.getAsBytes(Const.SECOND_KEY).length != EpidUtils.getEpidGroupIdLength(sgType)) {
      throw new InvalidMessageException("Invalid group ID from SigInfo");
    }

    ByteArrayOutputStream sigInfoBytes = new ByteArrayOutputStream();

    switch (sgType) {
      case Const.SG_EPIDv10:
      case Const.SG_EPIDv11:

        byte[] certBytes = Const.EMPTY_BYTE;
        try {
          certBytes = getGroupCertSigma10(sigA);
        } catch (RuntimeException ex) {
          System.out.println("Runtime Exception in getSigInfo");
          // intentional fall through
          // some EPID 1.1 groups have a cert 0 and others don't
        }
        sigInfoBytes.write(getLengthBytes(certBytes.length));
        if (certBytes.length > 0) {
          sigInfoBytes.write(certBytes);
        }

        certBytes = Const.EMPTY_BYTE;
        try {
          certBytes = getGroupCertSigma11(sigA);
        } catch (RuntimeException ex) {
          System.out.println("Runtime Exception in getSigInfo");
          // intentional fall through
        }
        sigInfoBytes.write(getLengthBytes(certBytes.length));
        if (certBytes.length > 0) {
          sigInfoBytes.write(certBytes);
        }

        byte[] sigRlBytes = Const.EMPTY_BYTE;
        try {
          sigRlBytes = getSigrl(sigA,
                  Const.EPID_PROTOCOL_VERSION_V2 + Const.URL_PATH_SEPARATOR +  Const.EPID_11);
        } catch (RuntimeException ex) {
          System.out.println("Runtime Exception in getSigInfo");
          // intentional fall through
        }
        sigInfoBytes.write(getLengthBytes(sigRlBytes.length));
        if (sigRlBytes.length > 0) {
          sigInfoBytes.write(sigRlBytes);
        }
        break;
      default:
        throw new IllegalArgumentException("EpidVersion is invalid.");
    }

    return Composite.newArray()
        .set(Const.FIRST_KEY, sgType)
        .set(Const.SECOND_KEY, sigInfoBytes.toByteArray());
  }

  /**
   * Converts int to byte string for EPID data array.
   * @param lengthValue length value
   * @return byte array containing length
   */
  public byte[] getLengthBytes(int lengthValue) {
    byte[] lengthBytes = new byte[2];
    lengthBytes[0] = (byte) (lengthValue / 256);
    lengthBytes[1] = (byte) (lengthValue % 256);
    return lengthBytes;
  }
}
