// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.message.SigInfo;
import org.fidoalliance.fdo.protocol.message.SigInfoType;

public class EpidService {

  private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
    Thread t = Executors.defaultThreadFactory().newThread(r);
    t.setDaemon(true);
    return t;
  });
  private static final long httpRequestTimeout = Duration.ofSeconds(10).getSeconds();

  private static final String defaultEpidOnlineUrl = "http://verifier.fdorv.com/";
  private static URI epidOnlineUrl = null;

  private static final int EpidGroupLength = 4;
  private static final String EPID_PROTOCOL_VERSION_V1 = "v1";
  private static final String EPID_PROTOCOL_VERSION_V2 = "v2";
  private static final String EPID_11 = "epid11";
  private static final String EPID_PROOF_URI_PATH = "proof";
  private static final String URL_PATH_SEPARATOR = "/";

  private static final String SIGRL = "SIGRL";
  private static final String GROUPCERTSIGMA10 = "PUBKEY.CRT.BIN";
  private static final String GROUPCERTSIGMA11 = "PUBKEY.CRT";

  // This is not defined in HttpUrlConnection
  public static final int HTTP_EXPECTATION_FAILED = 417;

  private static final LoggerService logger = new LoggerService(EpidService.class);

  protected static class RootConfig {

    @JsonProperty("epid")
    private EpidService.EpidConfig root;

    protected EpidService.EpidConfig getRoot() {
      return root;
    }
  }

  protected static class EpidConfig {

    @JsonProperty("url")
    private String path;
    @JsonProperty("testMode")
    private Boolean testMode;

    protected String getPath() {
      return Config.resolve(path);
    }

    protected Boolean getTestMode() {
      return testMode;
    }
  }


  protected EpidService.RootConfig config =
      Config.getConfig(EpidService.RootConfig.class);


  /**
   * Return the EPID Online Verification Service URL.
   *
   * @return {@link URI} instance
   */
  private URI getEpidOnlineUrl() {
    if (null == epidOnlineUrl) {
      if (config.getRoot().getPath() != null
          && !config.getRoot().getPath().isEmpty()) {
        epidOnlineUrl = URI.create(config.getRoot().getPath());
      } else {
        epidOnlineUrl = URI.create(defaultEpidOnlineUrl);
      }
    }
    return epidOnlineUrl;
  }

  private byte[] getSigrl(SigInfo sigInfo, String epidVersion) throws IOException {
    byte[] sigrlResponse = getEpidVerificationServiceResource(sigInfo, SIGRL, epidVersion);
    if (sigrlResponse == null) {
      return new byte[]{};
    }
    return sigrlResponse;
  }

  private byte[] getGroupCertSigma10(SigInfo sigInfo) {
    return getEpidVerificationServiceResource(
        sigInfo, GROUPCERTSIGMA10,
        (EPID_PROTOCOL_VERSION_V2 + URL_PATH_SEPARATOR + EPID_11));
  }

  private byte[] getGroupCertSigma11(SigInfo sigInfo) {
    return getEpidVerificationServiceResource(
        sigInfo, GROUPCERTSIGMA11,
        (EPID_PROTOCOL_VERSION_V2 + URL_PATH_SEPARATOR + EPID_11));
  }

  private byte[] getEpidVerificationServiceResource(
      SigInfo sigInfo, String resource, String epidVersion) {
    String path =
        String.join(URL_PATH_SEPARATOR,
            Arrays.asList(
                epidVersion,
                Hex.encodeHexString(sigInfo.getInfo()).toUpperCase(),
                resource.toLowerCase()));

    String url;
    try {
      url =
          new URL(getEpidOnlineUrl().toString())
              .toURI()
              .resolve(URL_PATH_SEPARATOR + path)
              .toString();
      return doGet(url);
    } catch (URISyntaxException | IOException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * EPID signature from verification service.
   *
   * @param sigA device group and epid type data
   * @return EPID eb data suitable for EPID provisioning
   * @throws IOException for unhandled IO Exceptions
   */
  public SigInfo getSigInfo(SigInfo sigA) throws IOException {

    if (sigA.getInfo().length != EpidGroupLength) {
      throw new IOException("Invalid group ID from SigInfo");
    }

    ByteArrayOutputStream sigInfoBytes = new ByteArrayOutputStream();

    byte[] certBytes = new byte[0];
    try {
      certBytes = getGroupCertSigma10(sigA);
    } catch (RuntimeException ex) {
      logger.error("Runtime Exception in getSigInfo");
      // intentional fall through
      // some EPID 1.1 groups have a cert 0 and others don't
    }
    sigInfoBytes.write(getLengthBytes(certBytes.length));
    if (certBytes.length > 0) {
      sigInfoBytes.write(certBytes);
    }

    certBytes = new byte[0];
    try {
      certBytes = getGroupCertSigma11(sigA);
    } catch (RuntimeException ex) {
      logger.error("Runtime Exception in getSigInfo");
      // intentional fall through
    }
    sigInfoBytes.write(getLengthBytes(certBytes.length));
    if (certBytes.length > 0) {
      sigInfoBytes.write(certBytes);
    }

    byte[] sigRlBytes = new byte[0];
    try {
      sigRlBytes = getSigrl(sigA,
          EPID_PROTOCOL_VERSION_V2 + URL_PATH_SEPARATOR + EPID_11);
    } catch (RuntimeException ex) {
      logger.error("Runtime Exception in getSigInfo");
      // intentional fall through
    }
    sigInfoBytes.write(getLengthBytes(sigRlBytes.length));
    if (sigRlBytes.length > 0) {
      sigInfoBytes.write(sigRlBytes);
    }

    SigInfo sigB = new SigInfo();
    sigB.setSigInfoType(sigA.getSigInfoType());
    sigB.setInfo(sigInfoBytes.toByteArray());
    return sigB;
  }

  /**
   * Converts int to byte string for EPID data array.
   *
   * @param lengthValue length value
   * @return byte array containing length
   */
  private byte[] getLengthBytes(int lengthValue) {
    byte[] lengthBytes = new byte[2];
    lengthBytes[0] = (byte) (lengthValue / 256);
    lengthBytes[1] = (byte) (lengthValue % 256);
    return lengthBytes;
  }

  /**
   * Verify epid signature.
   *
   * @param signature The signature to verify.
   * @param maroePrefix The maroePrefix.
   * @param nonce The nonce.
   * @param signedData The signed state.
   * @param groupId The Epid group ID.
   * @param sgType The sigInfo type.
   * @return True if the signature is valid.
   */
  public boolean verifyEpidSignature(byte[] signature,
      byte[] maroePrefix,
      byte[] nonce,
      byte[] signedData,
      byte[] groupId,
      SigInfoType sgType) {
    try {
      String msg = createEpidSignatureBodyMessage(
              signature,
              maroePrefix,
              nonce,
              signedData,
              groupId,
              sgType);

      String url = null;
      String path;

      path = String.join(EpidService.URL_PATH_SEPARATOR,
              Arrays.asList(EpidService.EPID_PROTOCOL_VERSION_V1,
                      EpidService.EPID_11, EpidService.EPID_PROOF_URI_PATH));
      url = new URL(
              getEpidOnlineUrl().toString())
              .toURI().resolve(EpidService.URL_PATH_SEPARATOR + path).toString();

      int response = doPost(url, msg);
      if (response != HttpURLConnection.HTTP_OK) {
        if (config.getRoot().getTestMode() != null
                && config.getRoot().getTestMode()) {
          // in test mode return false only for non-signature issues
          switch (response) {
            case HttpURLConnection.HTTP_FORBIDDEN:
              // invalid signature
            case HTTP_EXPECTATION_FAILED:
              // outdated sigrl
              return true;
            default:
              // malformed request or other reason
              return false;
          }
        }
        return false;
      }
    } catch (MalformedURLException ex) {
      return false;
    } catch (IOException ex) {
      return false;
    } catch (URISyntaxException ex) {
      return false;
    } catch (RuntimeException ex) {
      return false;
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private String createEpidSignatureBodyMessage(byte[] signature,
      byte[] maroePrefix,
      byte[] nonce,
      byte[] signedData,
      byte[] groupId,
      SigInfoType sgType) throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (sgType == SigInfoType.EPID10) {
      baos.write((byte) maroePrefix.length);
      baos.write(maroePrefix);
      baos.write(nonce);
      baos.write(signedData);
    } else if (sgType == SigInfoType.EPID11) {
      baos.write(ByteBuffer.allocate(48).put(4, (byte) 0x48).put(8, (byte) 0x08).array());
      baos.write(maroePrefix);
      baos.write(ByteBuffer.allocate(16).array());
      baos.write(nonce);
      baos.write(ByteBuffer.allocate(16).array());
      baos.write(signedData);
    } else {
      return null;
    }

    // EPID devices may return a signature is a slightly different format than that
    // expected by the EPID verifier. Adjust the signature for these cases here, before
    // building the body for the verifier.

    final int sigWithHeaderNoCounts = 569;
    final int sigNoHeaderNoCounts = 565;
    final int sigWithHeaderWithCounts = 573;

    // Conversion cases:
    // 1) (siglength == SIG_WITH_HEADER_NO_COUNTS)
    //    remove first 4 bytes and add 8 zeroes on the end
    //    sver and blobid are prepended and sigRLVersion and n2 are not included
    // 2) (sigLength == SIG_NO_HEADER_NO_COUNTS)
    //    add 8 zeroes on the end (signature is missing sigRLVersion and n2 values)
    // 3) ((sigLength - SIG_WITH_HEADER_WITH_COUNTS) % 160 == 4)
    //    remove first 4 bytes (sver and blobid)

    byte[] adjSignature = signature;
    if (signature.length == sigWithHeaderNoCounts) {
      adjSignature = new byte[signature.length + 4];
      System.arraycopy(signature, 4, adjSignature, 0, signature.length - 4);
    } else if (signature.length == sigNoHeaderNoCounts) {
      adjSignature = new byte[signature.length + 8];
      System.arraycopy(signature, 0, adjSignature, 0, signature.length);
    } else if (((signature.length - sigWithHeaderWithCounts) % 160) == 4) {
      adjSignature = new byte[signature.length - 4];
      System.arraycopy(signature, 4, adjSignature, 0, signature.length - 4);
    }

    // Create the JSON encoded message body to send to verifier
    String msg = "{"
        + "\"groupId\":\"" + Base64.getEncoder().encodeToString(groupId) + "\""
        + ",\"msg\":\"" + Base64.getEncoder().encodeToString(baos.toByteArray()) + "\""
        + ",\"epidSignature\":\"" + Base64.getEncoder().encodeToString(adjSignature) + "\""
        + "}";
    return msg;
  }


  /**
   * Perform HTTP GET operation.
   *
   * @param url the Url to which request will be sent
   * @return the response from the Url
   * @throws IOException throws an IOException in case the response status code is not 200 (OK)
   */
  private byte[] doGet(String url) throws IOException {
    java.net.http.HttpClient httpClient;
    try {
      httpClient = java.net.http.HttpClient.newBuilder().build();
      final HttpRequest.Builder httpRequestBuilder =
          HttpRequest.newBuilder().header("Accept", "application/octet-stream")
              .version(HttpClient.Version.HTTP_1_1);
      final HttpRequest httpRequest = httpRequestBuilder.uri(URI.create(url)).GET().build();
      final Future<HttpResponse<byte[]>> future =
          executor.submit(
              () -> httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray()));
      final HttpResponse<byte[]> httpResponse;
      httpResponse = future.get(httpRequestTimeout, TimeUnit.SECONDS);
      if (httpResponse.statusCode() == HttpURLConnection.HTTP_OK) {
        return null != httpResponse.body() ? httpResponse.body() : new byte[0];
      } else {
        throw new IOException("HTTP GET failed with: " + httpResponse.statusCode());
      }
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Perform HTTP POST operation.
   *
   * @param url     the Url to which request will be sent
   * @param payload the data to send in HTTP request body
   * @return the status code response from the Url
   * @throws IOException throws an IOException in case the response status code is not 200 (OK)
   */
  private int doPost(String url, String payload) throws IOException {
    java.net.http.HttpClient httpClient;
    try {
      httpClient = HttpClient.newBuilder().build();
      final HttpRequest.Builder httpRequestBuilder =
          HttpRequest.newBuilder().header("Content-Type", "application/json")
              .version(HttpClient.Version.HTTP_1_1);
      final HttpRequest httpRequest =
          httpRequestBuilder.uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(payload))
              .build();
      final Future<HttpResponse<String>> future =
          executor.submit(() -> httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()));
      final HttpResponse<String> httpResponse;
      httpResponse = future.get(httpRequestTimeout, TimeUnit.SECONDS);
      return httpResponse.statusCode();

    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

}
