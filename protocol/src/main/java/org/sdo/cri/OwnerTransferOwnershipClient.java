// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.CharBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import org.sdo.cri.RendezvousInstr.Only;
import org.sdo.cri.To0OwnerSignTo0dCodec.To0dEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The SDO Owner client, which runs TO0 to register the owner with the SDO rendezvous service.
 */
public class OwnerTransferOwnershipClient implements Callable<Optional<Duration>> {

  private final OwnershipVoucher113 myVoucher;
  private final HttpClient myHttpClient;
  private final Predicate<CertPath> myCertPathValidator;
  private final Function<OwnershipVoucher, Duration> myWaitSecondsBuilder;
  private final OwnerLocationInfo myLocationInfo;
  private final Function<KeyType, KeyPair> myKeysProvider;
  private final BiPredicate<OwnershipVoucher, Integer> myRetryPredicate;

  /**
   * Construct a new object.
   *
   * @param httpClient         The HttpClient to use for outgoing connections.
   * @param ownershipVoucher   The ownership voucher to register with the SDO service.
   * @param certPathValidator  A Predicate tested to validate any device certificate chain, may be
   *                           null.
   * @param ownerLocationInfo  The owner location info to advertise with the SDO service.
   * @param keysProvider       A provider of owner signing keys.
   * @param waitSecondsBuilder The factory providing our wait-second values.
   * @param retryPredicate     A predicate which, if it fails, will interrupt the retry loop.
   *                           Arguments are the voucher for which we are currently retrying and the
   *                           current retry count (0 = first try)
   */
  public OwnerTransferOwnershipClient(
      HttpClient httpClient,
      OwnershipVoucher ownershipVoucher,
      Predicate<CertPath> certPathValidator,
      OwnerLocationInfo ownerLocationInfo,
      Function<KeyType, KeyPair> keysProvider,
      Function<OwnershipVoucher, Duration> waitSecondsBuilder,
      BiPredicate<OwnershipVoucher, Integer> retryPredicate) {

    if (!(ownershipVoucher instanceof OwnershipVoucher113)) {
      throw new IllegalArgumentException();
    }
    myVoucher = (OwnershipVoucher113) ownershipVoucher;
    myHttpClient = Objects.requireNonNull(httpClient);
    myCertPathValidator = certPathValidator;
    myWaitSecondsBuilder = waitSecondsBuilder;
    myLocationInfo = ownerLocationInfo;
    myKeysProvider = keysProvider;
    myRetryPredicate = Objects.requireNonNull(retryPredicate);
  }

  /**
   * Construct a new object.
   *
   * @param httpClient         The HttpClient to use for outgoing connections.
   * @param ownershipVoucher   The ownership voucher to register with the SDO service.
   * @param certPathValidator  A Predicate tested to validate any device certificate chain, may be
   *                           null.
   * @param ownerLocationInfo  The owner location info to advertise with the SDO service.
   * @param keysProvider       A provider of owner signing keys.
   * @param waitSecondsBuilder The factory providing our wait-second values.
   */
  public OwnerTransferOwnershipClient(
      HttpClient httpClient,
      OwnershipVoucher ownershipVoucher,
      Predicate<CertPath> certPathValidator,
      OwnerLocationInfo ownerLocationInfo,
      Function<KeyType, KeyPair> keysProvider,
      Function<OwnershipVoucher, Duration> waitSecondsBuilder) {

    this(
        httpClient,
        ownershipVoucher,
        certPathValidator,
        ownerLocationInfo,
        keysProvider,
        waitSecondsBuilder,
        (voucher, count) -> (count < 3)); // if we're given no retry predicate, retry three times
  }

  @Override
  public Optional<Duration> call() throws InterruptedException, URISyntaxException {

    // The owner is expected to validate any device certificate path.
    // Since TO0 is the first time the owner runs, we do it here.
    CertPath deviceCertPath = myVoucher.getDc();
    if (!(null == myCertPathValidator || null == deviceCertPath)) {
      if (!myCertPathValidator.test(deviceCertPath)) {
        logger().error("certificate path is invalid");
        return Optional.empty();
      }
    }

    final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    Duration waitSeconds = null;
    long delay = -1;
    int retryCount = 0;
    while (null == waitSeconds && myRetryPredicate.test(myVoucher, retryCount++)) {
      for (RendezvousInstr rendezvous : myVoucher.getOh().getR()) {

        Iterator<URI> it = rendezvous.toUris(Only.owner).iterator();
        while (null == waitSeconds && it.hasNext()) {

          try {
            waitSeconds = to0(it.next());

          } catch (Exception e) {
            logger().error(e.getMessage());
            logger().debug(e.getMessage(), e);
          }
        } // while no waitseconds, more URIs to try

        if (null == waitSeconds && null != rendezvous.getDelay()) {

          delay = rendezvous.getDelay().toSeconds();
          logger().info("instruction contains delay. Pausing until "
              + dateTimeFormatter.format(Instant.now().plus(delay, ChronoUnit.SECONDS)));
          TimeUnit.SECONDS.sleep(delay);

        } else {
          delay = -1;
        }
      } // foreach rendezvous instruction

      // From the SDO Protocol Specification:
      //
      // If “delaysec” does not appear and the last entry in RendezvousInfo has been
      // processed, a delay of 120s +- random(30) is executed.
      final int delaySec = defaultDelay();
      final int jitterSec = defaultDelayJitter();

      if (null == waitSeconds && delay < 0) {
        delay = delaySec + ThreadLocalRandom.current().nextInt(-jitterSec, jitterSec);
        logger().info("All rendezvous instructions exhausted. Pausing until "
            + dateTimeFormatter.format(Instant.now().plus(delay, ChronoUnit.SECONDS)));
        TimeUnit.SECONDS.sleep(delay);
      }
    } // for waitSeconds != null && retries < max

    if (null == waitSeconds) {
      logger().warn("Retry limit reached for " + myVoucher.getUuid().toString());
    }

    logger().info("voucher "
        + myVoucher.getUuid()
        + " accepted by server until "
        + DateTimeFormatter.RFC_1123_DATE_TIME.format(
        Instant.now().plus(waitSeconds).atZone(ZoneId.systemDefault())));
    return Optional.ofNullable(waitSeconds);
  }

  private int defaultDelay() {
    return 120;
  }

  private int defaultDelayJitter() {
    return 30;
  }

  private <T> String encodeToString(T value, Codec<T>.Encoder encoder) {
    StringWriter w = new StringWriter();
    try {
      encoder.apply(w, value);
    } catch (IOException e) {
      // IO errors should never occur when writing to strings.  Treat this as fatal.
      throw new RuntimeException(e);
    }
    return w.toString();
  }

  private <T> String encodeToString(T value, ProtocolEncoder<T> encoder) {
    StringWriter w = new StringWriter();
    try {
      encoder.encode(w, value);
    } catch (IOException e) {
      // IO errors should never occur when writing to strings.  Treat this as fatal.
      throw new RuntimeException(e);
    }
    return w.toString();
  }

  private Logger logger() {
    return LoggerFactory.getLogger(getClass());
  }

  private Duration to0(URI serverUri) throws
      IOException,
      InterruptedException,
      InvalidKeyException,
      SignatureException,
      NoSuchAlgorithmException {

    final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .header(HttpUtil.CONTENT_TYPE, HttpUtil.APPLICATION_JSON);

    final To0Hello hello = new To0Hello();
    HttpRequest httpRequest = requestBuilder
        .uri(serverUri.resolve(HttpPath.of(hello)))
        .POST(BodyPublishers.ofString(encodeToString(hello, new To0HelloCodec().encoder())))
        .build();
    logger().info(HttpUtil.dump(httpRequest));
    HttpResponse<String> httpResponse = myHttpClient.send(httpRequest, BodyHandlers.ofString());
    logger().info(HttpUtil.dump(httpResponse));
    if (HttpUtil.OK_200 != httpResponse.statusCode()) {
      throw new IOException(httpResponse.toString() + " " + httpResponse.body());
    }
    final To0HelloAck helloAck =
        new To0HelloAckCodec().decoder().apply(CharBuffer.wrap(httpResponse.body()));

    final KeyType voucherKeyType = Keys.toType(myVoucher.getOh().getPk());
    final DigestService digestService =
        CryptoLevels.keyTypeToCryptoLevel(voucherKeyType).buildDigestService();
    To0OwnerSignTo0d to0d =
        new To0OwnerSignTo0d(myVoucher, myWaitSecondsBuilder.apply(myVoucher), helloAck.getN3());
    final HashDigest to0dh = digestService
        .digestOf(US_ASCII.encode(encodeToString(to0d, new To0dEncoder())));
    To1SdoRedirect redirect = new To1SdoRedirect(
        myLocationInfo.getInetAddress(),
        myLocationInfo.getHostname(),
        myLocationInfo.getPort(),
        to0dh);

    final String redirectText = encodeToString(redirect, new To1SdoRedirectCodec().encoder());
    final SignatureBlock to1d;
    try (KeyPairCloser keys = new KeyPairCloser(myKeysProvider.apply(voucherKeyType))) {
      to1d = new SignatureBlock(
          redirectText, null, Signatures.sign(redirectText, keys.getPrivate()));
    }

    final To0OwnerSign ownerSign = new To0OwnerSign(to0d, to1d);
    PublicKeyCodec.Encoder pkEncoder = new PublicKeyCodec.Encoder(myVoucher.getOh().getPe());
    SignatureBlockCodec.Encoder sgEncoder = new SignatureBlockCodec.Encoder(pkEncoder);
    To0OwnerSignCodec.Encoder ownerSignEncoder = new To0OwnerSignCodec.Encoder(sgEncoder);

    httpRequest = requestBuilder
        .uri(serverUri.resolve(HttpPath.of(ownerSign)))
        .header(
            HttpUtil.AUTHORIZATION,
            httpResponse.headers().firstValue(HttpUtil.AUTHORIZATION).orElse(""))
        .POST(BodyPublishers.ofString(encodeToString(ownerSign, ownerSignEncoder)))
        .build();
    logger().info(HttpUtil.dump(httpRequest));
    httpResponse = myHttpClient.send(httpRequest, BodyHandlers.ofString());
    logger().info(HttpUtil.dump(httpResponse));
    if (HttpUtil.OK_200 != httpResponse.statusCode()) {
      throw new IOException(httpResponse.toString() + " " + httpResponse.body());
    }
    final To0AcceptOwner acceptOwner =
        new To0AcceptOwnerCodec().decoder().apply(CharBuffer.wrap(httpResponse.body()));
    return acceptOwner.getWs();
  }
}
