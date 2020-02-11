// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the device side of the SDO DI protocol.
 */
public class DeviceInitializationClient implements Callable<DeviceCredentials> {

  private final URI myMfrUri;
  private final HttpClient myHttpClient;
  private final String myDeviceMark;
  private final HmacKeyFactory myHmacKeyFactory;

  /**
   * Construct a new object.
   *
   * @param httpClient            The {@link HttpClient} to use when sending HTTP requests.
   * @param deviceMark            The device's 'manufacturing/identifying mark'.
   * @param mfrUri                The {@link URI} at which to contact the manufacturer.
   * @param devicePubKey          The device's public key.
   * @param secureRandom          The SecureRandom to use during DI.
   */
  public DeviceInitializationClient(
      HttpClient httpClient,
      String deviceMark,
      URI mfrUri,
      PublicKey devicePubKey,
      SecureRandom secureRandom) {

    this.myHttpClient = Objects.requireNonNull(httpClient, "httpClient must be non-null");
    this.myDeviceMark = Objects.requireNonNull(deviceMark, "deviceMark must be non-null");
    if (myDeviceMark.isBlank()) {
      throw new IllegalArgumentException("deviceMark must not be blank");
    }

    this.myMfrUri =
        Objects.requireNonNull(mfrUri, "manufacturer (Device Initializaton) URI must be non-null");

    // If we obeyed the law of Demeter callers would pass the HmacKeyFactory itself.
    // The SDO protocol doesn't allow free selection of the HMAC algorithm, though,
    // so there's only ever one legal factory they could provide.
    // Therefore, it's this author's opinion that it makes more sense to have callers
    // pass in the values they DO have control over and to build the factory here.
    this.myHmacKeyFactory = new HmacKeyFactory(devicePubKey, secureRandom);
  }

  /**
   * Run the DI protocol to completion.
   *
   * @return Our generated SDO device credentials.
   */
  @Override
  public DeviceCredentials call() throws Exception {
    final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .header(HttpUtil.CONTENT_TYPE, HttpUtil.APPLICATION_JSON);
    StringWriter sw = new StringWriter();
    final DiAppStart dias = new DiAppStart(myDeviceMark);
    new DiAppStartCodec().encoder().apply(sw, dias);
    logger().info("DI URI is: " + myMfrUri);
    HttpRequest request = requestBuilder
        .uri(myMfrUri.resolve(HttpPath.of(dias)))
        .POST(BodyPublishers.ofString(sw.toString()))
        .build();
    logger().info(HttpUtil.dump(request));
    HttpResponse<String> response = myHttpClient.send(request, BodyHandlers.ofString());
    logger().info(HttpUtil.dump(response));
    if (HttpUtil.OK_200 != response.statusCode()) {
      throw new IOException(response.toString() + " " + response.body());
    }
    final DiSetCredentialsCodec.DiSetCredentialsDecoder discDecoder =
        new DiSetCredentialsCodec.DiSetCredentialsDecoder();
    final DiSetCredentials disc = discDecoder.decode(CharBuffer.wrap(response.body()));
    SecretKey hmacKey = myHmacKeyFactory.get();
    try {
      final MacService macSvc = new SimpleMacService(hmacKey);
      final DiSetHmac dish = new DiSetHmac(
          macSvc.macOf(StandardCharsets.US_ASCII.encode(discDecoder.getLastOh())));
      sw = new StringWriter();
      new DiSetHmacCodec().encoder().apply(sw, dish);
      String authorization = response.headers().firstValue(HttpUtil.AUTHORIZATION).orElse("");
      request = requestBuilder
          .uri(myMfrUri.resolve(HttpPath.of(dish)))
          .header(HttpUtil.AUTHORIZATION, authorization)
          .POST(BodyPublishers.ofString(sw.toString()))
          .build();
      logger().info(HttpUtil.dump(request));
      response = myHttpClient.send(request, BodyHandlers.ofString());
      logger().info(HttpUtil.dump(response));
      if (HttpUtil.OK_200 != response.statusCode()) {
        throw new IOException(response.toString());
      }
      new DiDoneCodec().decoder()
          .apply(CharBuffer.wrap(response.body())); // ignored, but must decode
      final OwnershipVoucherHeader oh = disc.getOh();
      final ManufacturerBlock m = new ManufacturerBlock(oh.getD());
      final DigestService digestSvc = CryptoLevels
          .keyTypeToCryptoLevel(Keys.toType(oh.getPk()))
          .buildDigestService();
      final OwnerBlock o = new OwnerBlock(
          oh.getPe(),
          oh.getG(),
          oh.getR(),
          digestSvc.digestOf(StandardCharsets.US_ASCII.encode(discDecoder.getLastPk())));
      return new DeviceCredentials113(DeviceState.READY1, hmacKey.getEncoded(), m, o);

    } finally {
      if (!(null == hmacKey || hmacKey.isDestroyed())) {
        try {
          hmacKey.destroy();
        } catch (DestroyFailedException e) {
          // This will probably happen, most security providers don't implement destroy().
          // No big deal.
        }
      }
    }
  }

  private static Logger logger() {
    return LoggerFactory.getLogger(DeviceInitializationClient.class);
  }
}
