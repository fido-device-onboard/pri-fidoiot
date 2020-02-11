// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class EpidOnlineVerifier {

  private static final Logger mlog = LoggerFactory.getLogger(EpidOnlineVerifier.class);
  private final URI epidServerUri;
  private final HttpClient httpClient;

  public EpidOnlineVerifier(URI epidServerUri, HttpClient httpClient) {
    this.epidServerUri = epidServerUri;
    this.httpClient = httpClient;
  }

  /**
   * Verify the provided data using the Epid Online Varification service.
   *
   * @param epidVersion - The EpidVersion for the verification
   * @param gid         - the group Id
   * @param msg         - the message to verify against
   * @param signature   - the signature to verify
   * @param lib         - EpidLibRev3 context to retrieve the URi
   * @return kEpidNoErr if good, otherwise kEpidErr
   */
  int verifyOnline(
      EpidLib.EpidVersion epidVersion,
      byte[] gid,
      byte[] msg,
      byte[] signature,
      EpidLib lib) throws IOException, InterruptedException {

    String verifierFile = "";
    switch (epidVersion) {
      case EPID_1_0:
      case EPID_1_1:
        verifierFile += "/v1/epid11/proof";
        break;

      case EPID_2_0:
        verifierFile += "/v1/epid20/proof";
        break;

      default:
        return EpidLib.EpidStatus.kEpidErr.getValue();
    }

    URI uri = epidServerUri.resolve(verifierFile);

    // Create the encoded JSON block to send
    String blk = "{"
        + "\"groupId\":\"" + Base64.getEncoder().encodeToString(gid) + "\""
        + ",\"msg\":\"" + Base64.getEncoder().encodeToString(msg) + "\""
        + ",\"epidSignature\":\"" + Base64.getEncoder().encodeToString(signature) + "\""
        + "}";

    //mlog.info("Epid Verify URI: " + uri.toString());
    //mlog.info("Epid Payload: " + blk);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header(HttpUtil.CONTENT_TYPE, "application/json")
        .POST(BodyPublishers.ofString(blk))
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

    // Allow the logging of the different responses from the documentation
    switch (response.statusCode()) {
      case 200:
        mlog.info("Online Verification - Successful");
        return EpidLib.EpidStatus.kEpidNoErr.getValue();
      case 400:
        mlog.info("Online Verification - Malformed Request");
        return EpidLib.EpidStatus.kEpidErr.getValue();
      case 403:
        mlog.info("Online Verification - Invalid Signature");
        return EpidLib.EpidStatus.kEpidErr.getValue();
      case 417:
        mlog.info("Online Verification - Outdated SigRl");
        return EpidLib.EpidStatus.kEpidErr.getValue();
      default:
        mlog.info("Online Verification - Unknown Error: " + response.statusCode());
        return EpidLib.EpidStatus.kEpidErr.getValue();
    }
  }
}
