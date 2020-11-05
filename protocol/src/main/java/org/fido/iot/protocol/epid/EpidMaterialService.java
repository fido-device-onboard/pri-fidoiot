// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.epid;

import java.io.ByteArrayOutputStream;
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

  /**
   * Combines list of byte array into a single byte array.
   *
   * @param data List of byte array containing EPIDresource data
   * @return byte array of all EPID resources
   * @throws IOException for unhadled IO exceptions
   */
  public static byte[] createLVs(List<byte[]> data) throws IOException {

    return (data.stream()
        .collect(
            () -> new ByteArrayOutputStream(),
            (b, e) -> {
              try {
                b.write(e);
              } catch (IOException ioException) {
                throw new RuntimeException(ioException);
              }
            },
            (a, b) -> {})
        .toByteArray());
  }

  private byte[] getSigrl(Composite sigInfo, String epidVersion) {
    byte[] sigrlResponse = getEpidVerificationServiceResource(sigInfo, Const.SIGRL, epidVersion);
    if (sigrlResponse == null) {
      return new byte[] {};
    }
    return sigrlResponse;
  }

  private byte[] getPublicKey(Composite sigInfo) {
    return getEpidVerificationServiceResource(
        sigInfo, Const.PUBKEY, (Const.EPID_PROTOCOL_VERSION_V2 + Const.EPID_20));
  }

  private byte[] getGroupCertSigma10(Composite sigInfo) {
    return getEpidVerificationServiceResource(
        sigInfo, Const.GROUPCERTSIGMA10, (Const.EPID_PROTOCOL_VERSION_V2 + Const.EPID_11));
  }

  private byte[] getGroupCertSigma11(Composite sigInfo) {
    return getEpidVerificationServiceResource(
        sigInfo, Const.GROUPCERTSIGMA11, (Const.EPID_PROTOCOL_VERSION_V2 + Const.EPID_11));
  }

  private byte[] getEpidVerificationServiceResource(
      Composite sigInfo, String resource, String epidVersion) {
    String path =
        String.join(
            Const.URL_PATH_SEPARATOR,
            Arrays.asList(
                epidVersion, Composite.toString(sigInfo.toBytes()), resource.toLowerCase()));

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
   * @param sigA inital device based information
   * @return actual signature from verification service
   * @throws IOException for unhandled IO Exceptions
   */
  public Composite getSigInfo(Composite sigA) throws IOException {

    int sgType = (int) sigA.getAsNumber(Const.FIRST_KEY);

    if (sigA.getAsBytes(Const.FIRST_KEY).length != EpidUtils.getEpidGroupIdLength(sgType)) {
      throw new InvalidMessageException("Invalid group ID from SigInfo");
    }

    List<byte[]> sigInfoBytes = new ArrayList<>();
    switch (sgType) {
      case Const.SG_EPIDv10:
        Composite certRequestSigInfo =
            Composite.newArray().set(Const.FIRST_KEY, sgType).set(Const.SECOND_KEY, sigA.toBytes());
        sigInfoBytes.add(getGroupCertSigma10(certRequestSigInfo));
        break;
      case Const.SG_EPIDv11:
        sigInfoBytes.add(getGroupCertSigma10(sigA));
        sigInfoBytes.add(getGroupCertSigma11(sigA));
        sigInfoBytes.add(getSigrl(sigA, Const.EPID_11));
        break;
      case Const.SG_EPIDv20:
        sigInfoBytes.add(getSigrl(sigA, Const.EPID_20));
        sigInfoBytes.add(getPublicKey(sigA));
        break;
      default:
        throw new IllegalArgumentException("EpidVersion is invalid.");
    }

    return Composite.newArray()
        .set(Const.FIRST_KEY, sigA.getAsComposite(Const.FIRST_KEY))
        .set(Const.SECOND_KEY, createLVs(sigInfoBytes));
  }
}
