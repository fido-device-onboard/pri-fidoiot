// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * EpidOnlineMaterial is a class to access the cryptographic material available online
 * though the Epid Online Verification Service.
 */
class EpidOnlineMaterial {

  private static final Logger mlog = LoggerFactory.getLogger(EpidOnlineMaterial.class);
  private final URI epidServerUri;
  private final HttpClient httpClient;

  /**
   * Constructor.
   */
  public EpidOnlineMaterial(URI epidServerUri, HttpClient httpClient) {
    this.epidServerUri = epidServerUri;
    this.httpClient = httpClient;
  }

  /**
   * Reads the online REST service for the requested materials.
   * Today these materials are all unsigned.
   *
   * @param gid      - the group ID, 4 or 16 bytes in length
   * @param epidType - Which version of Epid
   * @param matId    - Which material requested
   * @return a byte array containing the requested material of zero length if failed
   */
  public byte[] readEpidRestService(byte[] gid, EpidLib.EpidVersion epidType,
      EpidLib.MaterialId matId) throws InterruptedException, IOException {

    // Build the target from the passed values if they are valid
    String targetFile = "";
    switch (epidType) {
      case EPID_1_1:
        if (gid.length != 4) {
          throw new IllegalArgumentException();
        }
        targetFile += "/epid11/";
        break;

      case EPID_2_0:
        if (gid.length != 16) {
          throw new IllegalArgumentException();
        }
        targetFile += "/v2/epid20/";
        break;

      default:
        throw new RuntimeException("BUG: unexpected switch default");
    }

    String filename;
    switch (matId) {
      case SIGRL:
        filename = "sigrl";
        break;
      case PRIVRL:
        filename = "privrl";
        break;
      case PUBKEY:
        filename = "pubkey";
        break;
      case PUBKEY_CRT_BIN:
        filename = "pubkey.crt.bin";
        break;
      case PUBKEY_CRT:
        filename = "pubkey.crt";
        break;
      default:
        throw new IllegalArgumentException(
            "Incorrect material ID when reading material from Epid rest service: " + matId);
    }
    targetFile += Hex.toHexString(gid) + '/' + filename;
    mlog.debug("TargetFile: " + targetFile);

    URI uri = epidServerUri.resolve(targetFile);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header(HttpUtil.CONTENT_TYPE, "application/octet-stream")
        .GET()
        .build();
    HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());
    if (200 == response.statusCode()) {
      return response.body();
    } else {
      throw new IOException(response.toString());
    }
  }
}
