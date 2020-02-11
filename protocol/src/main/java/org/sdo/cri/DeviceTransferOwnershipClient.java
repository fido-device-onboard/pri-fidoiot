// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.sdo.cri.CipherUtils.decipherAndDecode;
import static org.sdo.cri.CipherUtils.encodeAndEncipher;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.sdo.cri.Buffers.Eraser;
import org.sdo.cri.CryptoLevels.CryptoLevel;
import org.sdo.cri.OwnershipVoucherHeaderCodec.OwnershipProxyHeaderEncoder;
import org.sdo.cri.RendezvousInstr.Only;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceTransferOwnershipClient implements Callable<Optional<DeviceCredentials>> {

  private final CipherBlockMode myCipherBlockMode;
  private final DeviceCredentials113 myDeviceCredentials;
  private final HttpClient myHttpClient;
  private final SecureRandom mySecureRandom;
  private final Collection<?> myServiceInfoModules;
  private Supplier<KeyPair> myKeysSupplier;

  /**
   * Construct a new object.
   *
   * @param cipherBlockMode    The block mode for the TO2 cipher.
   * @param dc                 Our device credentials.
   * @param httpClient         The HttpClient to use for outgoing connections.
   * @param secureRandom       The SecureRandom from which to get randomness.
   * @param serviceInfoModules The SDO ServiceInfo modules to use.
   * @param keysSupplier       The Supplier of the device's keys.
   */
  public DeviceTransferOwnershipClient(
      CipherBlockMode cipherBlockMode,
      DeviceCredentials dc,
      HttpClient httpClient,
      SecureRandom secureRandom,
      Collection<?> serviceInfoModules,
      Supplier<KeyPair> keysSupplier) {

    if (dc instanceof DeviceCredentials113) {
      myDeviceCredentials = Objects.requireNonNull((DeviceCredentials113) dc);
    } else {
      throw new UnsupportedOperationException();
    }
    myHttpClient = Objects.requireNonNull(httpClient);
    mySecureRandom = Objects.requireNonNull(secureRandom);
    myCipherBlockMode = Objects.requireNonNull(cipherBlockMode);
    myServiceInfoModules = Objects.requireNonNull(serviceInfoModules);
    myKeysSupplier = Objects.requireNonNull(keysSupplier);
  }

  private CipherType buildCs(KeyExchangeType kx) {
    switch (kx) {

      case DHKEXid14:
      case ECDH:
        return new CipherType(CipherAlgorithm.AES128, myCipherBlockMode,
            MacType.HMAC_SHA256);

      case DHKEXid15:
      case ECDH384:
        return new CipherType(CipherAlgorithm.AES256, myCipherBlockMode,
            MacType.HMAC_SHA384);

      case ASYMKEX:
      case ASYMKEX3072:
        // This device will never request ASYMKEX, we should never be here
      default:
        throw new RuntimeException("unexpected switch default");
    }
  }

  private KeyExchangeType buildKx(KeyType keyType) {
    switch (keyType) {
      case RSA2048RESTR:
        return KeyExchangeType.DHKEXid14;
      case RSA_UR:
        return KeyExchangeType.DHKEXid15;
      case ECDSA_P_256:
        return KeyExchangeType.ECDH;
      case ECDSA_P_384:
        return KeyExchangeType.ECDH384;
      default:
        throw new RuntimeException("unexpected switch default");
    }
  }

  private URI buildUri(String scheme, String host, int port) {
    try {
      // SDO uses 0 for 'default port', but URI uses -1
      return new URI(scheme, null, host, 0 == port ? -1 : port, null, null, null);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  @Override
  public Optional<DeviceCredentials> call()
      throws URISyntaxException, InterruptedException, IOException, ExecutionException {

    final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    // Run TO1 to get our redirection information.
    // Rendezvous information is available in our device credentials.
    RendezvousInfo rendezvousInfo = myDeviceCredentials.getO().getR();
    SignatureBlock to1Redirect = null;

    while (null == to1Redirect) {

      long delay = -1;

      for (RendezvousInstr rendezvous : rendezvousInfo) {

        logger().debug("TO1 rendezvous instruction is " + rendezvous.toString());

        // rendezvous instructions can be equivalent to more than one URI
        Iterator<URI> it = rendezvous.toUris(Only.dev).iterator();
        while (null == to1Redirect && it.hasNext()) {
          try {
            to1Redirect = to1(it.next());
          } catch (ProtocolException e) {
            final String errorText = "SDO rendezvous service says: " + e.getError().getEm();
            if (ErrorCode.ResourceNotFound == e.getError().getEc()) {
              logger().info(errorText); // these are not worrisome, owner hasn't finished TO0.
            } else {
              logger().warn(errorText);
            }
          } catch (Exception e) {
            logger().error(e.getMessage());
          }
        }

        if (null == to1Redirect && null != rendezvous.getDelay()) {

          delay = rendezvous.getDelay().toSeconds();
          logger().info("instruction contains delay. Pausing until "
              + dateTimeFormatter.format(Instant.now().plus(delay, ChronoUnit.SECONDS)));
          TimeUnit.SECONDS.sleep(delay);

        } else {
          delay = -1;
        }
      }

      // From the SDO Protocol Specification:
      //
      // If “delaysec” does not appear and the last entry in RendezvousInfo has been
      // processed, a delay of 120s +- random(30) is executed.
      final int delaySec = 120;
      final int jitterSec = 30;

      if (null == to1Redirect && delay < 0) {
        delay = delaySec + ThreadLocalRandom.current().nextInt(-jitterSec, jitterSec);
        logger().info("All rendezvous instructions exhausted. Pausing until "
            + dateTimeFormatter.format(Instant.now().plus(delay, ChronoUnit.SECONDS)));
        TimeUnit.SECONDS.sleep(delay);
      }
    }

    // The signature on the redirection can't be checked until TO2.ProveOPHdr, see
    // SDO Protocol Specification 5.6.3.
    final SignatureBlock finalTo1Redirect = to1Redirect;
    final To1SdoRedirect redirect =
        new To1SdoRedirectCodec().decoder().apply(CharBuffer.wrap(finalTo1Redirect.getBo()));

    // Run TO2 to provision the device.
    final String ownerHost;
    if (!(null == redirect.getDns1() || redirect.getDns1().isEmpty())) {
      ownerHost = redirect.getDns1();

    } else if (null != redirect.getI1()) {
      ownerHost = redirect.getI1().getHostAddress();

    } else {
      throw new IllegalArgumentException("illegal redirect host");
    }

    // Protocol information isn't available in the redirect, so try HTTP and HTTPS.
    // Only one of the two will ever work.  When one finishes, cancel the other.
    List<URI> to2Uris = List.of("http", "https").stream()
        .map(scheme -> buildUri(scheme, ownerHost, redirect.getPort1()))
        .collect(Collectors.toList());

    ExecutorService executor = Executors.newCachedThreadPool();
    try {
      CompletionService<Optional<DeviceCredentials>> cs = new ExecutorCompletionService<>(executor);
      List<Future> futures = new ArrayList<>(to2Uris.size());
      try {
        to2Uris.forEach(uri ->
            futures.add(cs.submit(() ->
                to2(uri, finalTo1Redirect))));
        for (int i = futures.size(); i > 0; i--) {
          try {
            Future<Optional<DeviceCredentials>> future = cs.take();
            return future.get();

          } catch (ExecutionException e) {
            // We expect all but one to throw this, so it's not very concerning.
            logger().debug(e.getMessage());
          }
        }
      } finally {
        futures.forEach(future -> future.cancel(true));
      }
    } finally {
      executor.shutdownNow();
    }
    return null; // all attempts failed, rats!
  }

  private HttpResponse<String> httpPost(URI uri, String authorization, String body)
      throws IOException, InterruptedException {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .header(HttpUtil.CONTENT_TYPE, HttpUtil.APPLICATION_JSON)
        .uri(uri)
        .POST(BodyPublishers.ofString(body));
    if (!(null == authorization || authorization.isBlank())) {
      requestBuilder = requestBuilder.header(HttpUtil.AUTHORIZATION, authorization);
    }

    HttpRequest request = requestBuilder.build();
    logger().info(HttpUtil.dump(request));
    HttpResponse<String> response = myHttpClient.send(request, BodyHandlers.ofString());
    logger().info(HttpUtil.dump(response));

    if (HttpUtil.OK_200 == response.statusCode()) {
      return response;
    } else {
      // If we receive an error code, check the body for an SDO error message which we can
      // throw as an ProtocolException.
      final Error err = parseSdoError(response.body());
      if (null != err) {
        throw new ProtocolException(err);
      } else {
        throw new IOException(response.toString() + " " + response.body());
      }
    }
  }

  private Logger logger() {
    return LoggerFactory.getLogger(getClass());
  }

  private Error parseSdoError(String s) {
    try {
      return new ErrorCodec().decoder().apply(CharBuffer.wrap(s));
    } catch (Exception e) {
      return null;
    }
  }

  private SignatureBlock to1(URI uri)
      throws InterruptedException,
      InvalidKeyException,
      IOException,
      NoSuchAlgorithmException,
      SignatureException {

    try (KeyPairCloser keys = new KeyPairCloser(myKeysSupplier.get())) {
      return to1(uri, keys.getPublic(), keys.getPrivate());
    }
  }

  private SignatureBlock to1(URI uri, PublicKey publicKey, PrivateKey privateKey)
      throws InterruptedException,
      InvalidKeyException,
      IOException,
      NoSuchAlgorithmException,
      SignatureException {

    final UUID g2 = myDeviceCredentials.getO().getG();
    SigInfo ea = new SigInfoFactory().build(publicKey);

    final To1HelloSdo hello = new To1HelloSdo(g2, ea);
    StringWriter sw = new StringWriter();
    new To1HelloSdoCodec().encoder().apply(sw, hello);
    HttpResponse<String> httpResponse =
        httpPost(uri.resolve(HttpPath.of(hello)), null, sw.toString());

    final To1HelloSdoAck helloAck =
        new To1HelloSdoAckCodec().decoder().apply(CharBuffer.wrap(httpResponse.body()));
    // We don't use eB, since our key is ECDSA

    // Non-EPID keys don't use ai, and we don't support EPID keys, so set empty buffer for that
    final To1ProveToSdo prove =
        new To1ProveToSdo(new byte[]{}, helloAck.getN4(), g2);

    final StringWriter bo = new StringWriter();
    new To1ProveToSdoCodec().encoder().apply(bo, prove);
    final SignatureBlock sb =
        new SignatureBlock(bo.toString(), publicKey, Signatures.sign(bo.toString(), privateKey));

    // TO1.ProveToSDO.pk is always null if non-epid (which we are)
    final SignatureBlockCodec.Encoder sbEncoder =
        new SignatureBlockCodec.Encoder(new PublicKeyCodec.Encoder(KeyEncoding.NONE));
    sw = new StringWriter();
    sbEncoder.encode(sw, new SignatureBlock(sb.getBo(), null, sb.getSg()));

    httpResponse = httpPost(
        uri.resolve(HttpPath.of(prove)),
        httpResponse.headers().firstValue(HttpUtil.AUTHORIZATION).orElse(null),
        sw.toString());

    // We cannot validate this signature at this point, as we don't get the owner's public
    // key until TO2.  Trust that the TO2 client will check this signature when able.
    return new SignatureBlockCodec.Decoder(null).decode(CharBuffer.wrap(
        Objects.requireNonNull(httpResponse.body())));
  }

  private Optional<DeviceCredentials> to2(URI uri, SignatureBlock to1Redirect)
      throws IOException,
      InterruptedException,
      ParseException,
      InvalidKeyException,
      HmacVerificationException,
      NoSuchAlgorithmException,
      SignatureException {

    try (KeyPairCloser keys = new KeyPairCloser(myKeysSupplier.get())) {
      return to2(uri, to1Redirect, keys.getPublic(), keys.getPrivate());
    }
  }

  private Optional<DeviceCredentials> to2(
      URI uri, SignatureBlock to1Redirect, PublicKey publicKey, PrivateKey privateKey)
      throws IOException,
      InterruptedException,
      ParseException,
      InvalidKeyException,
      HmacVerificationException,
      NoSuchAlgorithmException,
      SignatureException {

    // ----- PHASE 1: Validate the Ownership Proxy ------------------------------------------------

    final DeviceCredentials113 oldCredentials = myDeviceCredentials;

    final UUID g2 = oldCredentials.getO().getG();
    final Nonce n5 = new Nonce(mySecureRandom);
    final KeyEncoding pe = oldCredentials.getO().getPe();
    final SigInfo ea = new SigInfoFactory().build(publicKey);

    final KeyExchangeType kx = buildKx(Keys.toType(publicKey));
    final CipherType cs = buildCs(kx);
    To2HelloDevice hello = new To2HelloDevice(g2, n5, pe, kx, cs, ea);

    StringWriter sw = new StringWriter();
    new To2HelloDeviceCodec().encoder().apply(sw, hello);
    HttpResponse<String> httpResponse =
        httpPost(uri.resolve(HttpPath.of(hello)), null, sw.toString());
    final SignatureBlock signedProveOpHdr =
        new SignatureBlockCodec.Decoder(null).decode(CharBuffer.wrap(httpResponse.body()));

    boolean verified = Signatures.verify(
        signedProveOpHdr.getBo(), signedProveOpHdr.getSg(), signedProveOpHdr.getPk());
    if (!verified) {
      throw new ProtocolException(new Error(
          ErrorCode.MessageRefused, MessageType.TO2_PROVE_OP_HDR, "TO1.ProveOPHdr invalid"));
    }

    // Now that we have the owner key, we must verify the signature on the redirect that
    // sent us into TO2 in the first place.  See SDO Protocol Specification v1.12l, 5.6.2.
    verified = Signatures.verify(
        to1Redirect.getBo(), to1Redirect.getSg(), signedProveOpHdr.getPk());
    if (!verified) {
      throw new ProtocolException(new Error(
          ErrorCode.MessageRefused, MessageType.TO1_SDO_REDIRECT, "TO1.SDORedirect invalid"));
    }

    final To2ProveOpHdrCodec.Decoder to2ProveOpHdrDec = new To2ProveOpHdrCodec().new Decoder();
    final To2ProveOpHdr to2ProveOpHdr =
        to2ProveOpHdrDec.apply(CharBuffer.wrap(signedProveOpHdr.getBo()));

    // The protocol specification does not specify an order for these tests, so they are performed
    // in field order.
    //

    // To test the MAC, repeat it using the same algorithm
    final SecretKey macKey = new SecretKeySpec(
        oldCredentials.getSecret(),
        to2ProveOpHdr.getHmac().getType().getJceName());
    final HashMac actualMac =
        new SimpleMacService(macKey).macOf(US_ASCII.encode(to2ProveOpHdrDec.getLastOh()));

    if (!Objects.equals(Objects.requireNonNull(to2ProveOpHdr).getHmac(), actualMac)) {
      throw new ProtocolException(new Error(
          ErrorCode.MessageRefused, to2ProveOpHdr.getType(),
          "TO2.ProveOPHdr.bo.hmac invalid"));
    }

    if (!hello.getN5().equals(to2ProveOpHdr.getN5())) {
      throw new ProtocolException(new Error(
          ErrorCode.MessageRefused, to2ProveOpHdr.getType(), "TO2.ProveOPHdr.bo.n5 invalid"));
    }

    // We do not use EPID keys, so eB is not used or checked.

    // Per protocol specification:
    //
    // The Device TEE verifies the ownership proxy entries incrementally as follows:
    //   Variables:
    //     hc
    //       hash of GUID and DeviceInfo...
    //       SHA256[TO2.ProveOPHdr.bo.oh.g||TO2.ProveOPHdr.bo.oh.d]
    //     hp
    //       hash of previous entry
    //       (initialize with SHA[TO2.ProveOPHdr.bo.oh||TO2.ProveOpHdr.bo.hmac] )
    //     pk
    //       public key signed in previous entry (initialize with TO2.ProveOPHdr.bo.oh.pk)
    PublicKey ownerPubKey = to2ProveOpHdr.getOh().getPk();
    final DigestService digestService = CryptoLevels
        .keyTypeToCryptoLevel(Keys.toType(ownerPubKey))
        .buildDigestService();
    HashDigest hc = digestService.digestOf(
        US_ASCII.encode(to2ProveOpHdrDec.getLastG()),
        US_ASCII.encode(to2ProveOpHdrDec.getLastD()));
    HashDigest hp = digestService.digestOf(
        US_ASCII.encode(to2ProveOpHdrDec.getLastOh()),
        US_ASCII.encode(to2ProveOpHdrDec.getLastHmac()));

    Codec<To2GetOpNextEntry> getOpNextEntryCodec = new To2GetOpNextEntryCodec();
    final To2OpNextEntryCodec.Decoder opNextEntryDecoder = new To2OpNextEntryCodec.Decoder();
    final OwnershipVoucherEntryCodec.Decoder entryDecoder =
        new OwnershipVoucherEntryCodec.Decoder();
    final int sz = to2ProveOpHdr.getSz();

    for (int enn = 0; enn < sz; ++enn) {

      final To2GetOpNextEntry getOpNextEntry = new To2GetOpNextEntry(enn);
      sw = new StringWriter();
      getOpNextEntryCodec.encoder().apply(sw, getOpNextEntry);
      httpResponse = httpPost(
          uri.resolve(HttpPath.of(getOpNextEntry)),
          httpResponse.headers().firstValue(HttpUtil.AUTHORIZATION).orElse(null),
          sw.toString());
      final To2OpNextEntry opNextEntry =
          opNextEntryDecoder.decode(CharBuffer.wrap(httpResponse.body()));

      // Per protocol specification:
      //
      // For each entry:
      // Verify signature TO2.OPNextEntry.eni.sg using variable pk.
      // Verify variable hc matches TO2.OPNextEntry.eni.bo.hc
      // Verify hp matches TO2.OpNextEntry.eni.bo.hp
      // Update variable pk = TO2.OPNextEntry.eni.bo.pk
      // Update variable hp = SHA256[TO2.OpNextEntry.eni.bo]
      // If enn == TO2.ProveOpHdr.bo.sz-1 then verify TO2.ProveOPHdr.pk == TO2.OpNextEntry.eni.bo.pk
      verified = Signatures.verify(
          opNextEntry.getEni().getBo(), opNextEntry.getEni().getSg(), ownerPubKey);
      if (!verified) {
        throw new ProtocolException(new Error(
            ErrorCode.MessageRefused, opNextEntry.getType(),
            "TO2.OPNextEntry.eni.sg invalid"));
      }

      OwnershipVoucherEntry en = entryDecoder.decode(CharBuffer.wrap(opNextEntry.getEni().getBo()));

      if (!Objects.equals(hc, en.getHc())) {
        throw new ProtocolException(new Error(
            ErrorCode.MessageRefused, opNextEntry.getType(),
            "TO2.OPNextEntry.eni.bo.hc invalid"));
      }

      if (!Objects.equals(hp, en.getHp())) {
        throw new ProtocolException(new Error(
            ErrorCode.MessageRefused, opNextEntry.getType(),
            "TO2.OPNextEntry.eni.bo.hp invalid"));
      }

      ownerPubKey = en.getPk();
      hp = digestService.digestOf(US_ASCII.encode(opNextEntry.getEni().getBo()));

      if (opNextEntry.getEnn() == sz - 1) {

        if (!ownerPubKey.equals(signedProveOpHdr.getPk())) {
          throw new ProtocolException(new Error(
              ErrorCode.MessageRefused,
              opNextEntry.getType(),
              "TO2.ProveOPHdr.pk != TO2.OpNextEntry.eni.bo.pk"));
        }
      }
    }

    // ----- PHASE 2: Prove Device & Device Service Info ------------------------------------------

    final KeyExchange keyExchange;
    switch (kx) {
      case DHKEXid14:
        keyExchange = new DiffieHellmanKeyExchange.Group14(mySecureRandom);
        break;

      case DHKEXid15:
        keyExchange = new DiffieHellmanKeyExchange.Group15(mySecureRandom);
        break;

      case ECDH:
        keyExchange = new EcdhKeyExchange.P256.Device(mySecureRandom);
        break;

      case ECDH384:
        keyExchange = new EcdhKeyExchange.P384.Device(mySecureRandom);
        break;

      case ASYMKEX:
      case ASYMKEX3072:
        // This device will never request ASYMKEX, so it should be impossible to be here.
      default:
        throw new RuntimeException("unexpected switch default");  // if we got here, bug.
    }
    final ByteBuffer xb;
    try {
      xb = keyExchange.getMessage();

    } catch (InvalidAlgorithmParameterException | IOException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    final Nonce n7 = new Nonce(mySecureRandom);

    ServiceInfoMarshaller serviceInfoMarshaller = new ServiceInfoMarshaller();
    List<ServiceInfoSource> serviceInfoSources = myServiceInfoModules.stream()
        .filter(o -> o instanceof ServiceInfoSource)
        .map(ServiceInfoSource.class::cast)
        .collect(Collectors.toList());
    serviceInfoMarshaller.setSources(serviceInfoSources);
    final Iterable<Supplier<ServiceInfo>> deviceServiceInfos = serviceInfoMarshaller.marshal();

    int nn = 0;

    for (Object ignored : deviceServiceInfos) {
      ++nn;
    }

    final To2ProveDevice proveDevice = new To2ProveDevice(
        ByteBuffer.allocate(0), // ECDSA devices don't use ai
        to2ProveOpHdr.getN6(),
        n7,
        oldCredentials.getO().getG(),
        nn,
        xb);

    sw = new StringWriter();
    new To2ProveDeviceCodec().encoder().apply(sw, proveDevice);
    final SignatureBlock proveDeviceSigned = new SignatureBlock(
        sw.toString(), publicKey, Signatures.sign(sw.toString(), privateKey));

    // TO2.ProveDevice.pk is to be null if device is not EPID.  We aren't EPID.
    SignatureBlock sg =
        new SignatureBlock(proveDeviceSigned.getBo(), null, proveDeviceSigned.getSg());
    sw = new StringWriter();
    new SignatureBlockCodec.Encoder(new PublicKeyCodec.Encoder(KeyEncoding.NONE)).encode(sw, sg);
    final String proveDeviceText = sw.toString();

    httpResponse = httpPost(
        uri.resolve(HttpPath.of(proveDevice)),
        httpResponse.headers().firstValue(HttpUtil.AUTHORIZATION).orElse(null),
        proveDeviceText);

    // All messages after TO2.ProveDevice are to be enciphered.
    // @see "SDO Protocol Specification, 3.5.5. Key Exchange in the TO2 Protocol"

    final ProtocolCipher cipher;
    final EncryptedMessageCodec encryptedMessageCodec;
    try (Eraser eraser = new Eraser(keyExchange.generateSharedSecret(to2ProveOpHdr.getXa()))) {

      CryptoLevel cryptoLevel = CryptoLevels.keyExchangeTypeToCryptoLevel(keyExchange.getType());
      SecretKey sek = cryptoLevel.getSekDerivationFunction()
          .apply(Buffers.unwrap(eraser.getBuf()));
      switch (cs.getMode()) {
        case CTR:
          cipher = new CtrCipher(sek, mySecureRandom);
          break;
        case CBC:
          cipher = new CbcCipher(sek, mySecureRandom);
          break;
        default:
          throw new UnsupportedOperationException();
      }

      SecretKey svk = cryptoLevel.getSvkDerivationFunction()
          .apply(Buffers.unwrap(eraser.getBuf()));
      encryptedMessageCodec = new EncryptedMessageCodec(svk);

    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }

    for (Supplier<ServiceInfo> serviceInfoSupplier : deviceServiceInfos) {

      final To2GetNextDeviceServiceInfo getNextDeviceServiceInfo = decipherAndDecode(
          httpResponse.body(),
          encryptedMessageCodec,
          cipher,
          new To2GetNextDeviceServiceInfoCodec().decoder());

      ServiceInfo serviceInfo = serviceInfoSupplier.get();
      To2NextDeviceServiceInfo nextInfo =
          new To2NextDeviceServiceInfo(getNextDeviceServiceInfo.getNn(), serviceInfo);

      final String cryptText = encodeAndEncipher(
          nextInfo,
          new To2NextDeviceServiceInfoCodec().encoder(),
          cipher,
          encryptedMessageCodec);

      httpResponse = httpPost(
          uri.resolve(HttpPath.of(nextInfo)),
          httpResponse.headers().firstValue(HttpUtil.AUTHORIZATION).orElse(null),
          cryptText);
    }

    // ----- PHASE 3: Setup Device & Owner Service Info -------------------------------------------

    // We will need the hash of the pk field in TO2.SetupDevice for our new credential block
    final AtomicReference<CharBuffer> pkText = new AtomicReference<>();
    SignatureBlockCodec.Decoder sgDecoder = new SignatureBlockCodec.Decoder(pkText::set);
    To2SetupDeviceCodec.Decoder setupDeviceDecoder = new To2SetupDeviceCodec.Decoder(sgDecoder);
    final To2SetupDevice setupDevice = decipherAndDecode(
        httpResponse.body(),
        encryptedMessageCodec,
        cipher,
        setupDeviceDecoder);
    final HashDigest pkh = digestService.digestOf(US_ASCII.encode(pkText.get()));

    SignatureBlock signedNoh = setupDevice.getNoh();

    verified = Signatures.verify(signedNoh.getBo(), signedNoh.getSg(), signedNoh.getPk());
    if (!verified) {
      throw new ProtocolException(new Error(
          ErrorCode.MessageRefused, setupDevice.getType(), "TO2.SetupDevice.noh.sg invalid"));
    }

    final To2SetupDeviceNoh setupDeviceNoh =
        new To2SetupDeviceNohCodec().decoder().apply(CharBuffer.wrap(signedNoh.getBo()));

    if (!n7.equals(setupDeviceNoh.getN7())) {
      throw new ProtocolException(new Error(
          ErrorCode.MessageRefused, setupDevice.getType(),
          "TO2.SetupDevice.noh.bo.n7 invalid"));
    }

    List<ServiceInfoSink> serviceInfoSinks = myServiceInfoModules.stream()
        .filter(o -> o instanceof ServiceInfoSink)
        .map(ServiceInfoSink.class::cast)
        .collect(Collectors.toList());

    for (int n = 0; n < setupDevice.getOsinn(); ++n) {

      final To2GetNextOwnerServiceInfo getNextInfo = new To2GetNextOwnerServiceInfo(n);
      final String cryptText = encodeAndEncipher(
          getNextInfo,
          new To2GetNextOwnerServiceInfoCodec().encoder(),
          cipher,
          encryptedMessageCodec);
      httpResponse = httpPost(
          uri.resolve(HttpPath.of(getNextInfo)),
          httpResponse.headers().firstValue(HttpUtil.AUTHORIZATION).orElse(null),
          cryptText);

      final To2OwnerServiceInfo ownerInfo = decipherAndDecode(
          httpResponse.body(),
          encryptedMessageCodec,
          cipher,
          new To2OwnerServiceInfoCodec().decoder());

      for (Entry<CharSequence, CharSequence> entry : ownerInfo.getSv()) {
        for (ServiceInfoSink sink : serviceInfoSinks) {
          sink.putServiceInfo(entry);
        }
      }
    }

    // Check for the 'reuse protocol'
    //
    // From the protocol spec:
    //
    // <quote>
    // If
    //   TO2.SetupDevice.noh.bo.g3 == TO2.ProveOPHdr.bo.oh.g # GUID same as previous,
    //   and TO2.SetupDevice.noh.bo.r3[0] == TO2.ProveOPHdr.bo.oh.r # RendezvousInfo same as
    //     previous,
    //   and TO2.SetupDevice.pk == Owner’s current public key # public key in the last entry of
    //     Ownership Voucher,
    //   and TO2.SetupDevice.sig is a valid signature
    // Then
    //   Device does not update the Ownership Credential,
    //   and Device does not internally change the HMAC,
    //   and in TO2.Done message, devices responds with TO2.Done.hmac equal to the ASCII string “=”
    // </quote>
    boolean a = Objects.equals(setupDeviceNoh.getG3(), to2ProveOpHdr.getOh().getG());
    boolean b = Objects.equals(setupDeviceNoh.getR3(), to2ProveOpHdr.getOh().getR());
    boolean c = Objects.equals(signedNoh.getPk(), ownerPubKey);
    final boolean isReuse = a
        && b
        && c;

    final To2Done to2Done;
    final Optional<DeviceCredentials> result;
    if (isReuse) {
      logger().info("reuse protocol is enabled");
      to2Done =
          new To2Done(new HashMac(MacType.NONE, "=".getBytes(US_ASCII)), to2ProveOpHdr.getN6());
      result = Optional.empty();

    } else {
      final SecretKey newHmacKey = new HmacKeyFactory(publicKey, mySecureRandom).get();
      final DeviceCredentials113 newCredentials = new DeviceCredentials113(
          DeviceState.READYN,
          newHmacKey.getEncoded(),
          oldCredentials.getM(),
          oldCredentials.getO());

      newCredentials.getO().setR(setupDeviceNoh.getR3());
      newCredentials.getO().setG(setupDeviceNoh.getG3());
      newCredentials.getO().setPkh(pkh);

      OwnershipVoucherHeader oldHeader = to2ProveOpHdr.getOh();
      OwnershipVoucherHeader newHeader = new OwnershipVoucherHeader(
          oldHeader.getPe(),
          setupDeviceNoh.getR3(),
          setupDeviceNoh.getG3(),
          oldHeader.getD(),
          signedNoh.getPk(),
          oldHeader.getHdc());

      sw = new StringWriter();
      new OwnershipProxyHeaderEncoder().encode(sw, newHeader);
      final HashMac newHmac =
          new SimpleMacService(newHmacKey).macOf(US_ASCII.encode(sw.toString()));
      to2Done = new To2Done(newHmac, to2ProveOpHdr.getN6());
      result = Optional.of(newCredentials);
    }

    final String cryptText = encodeAndEncipher(
        to2Done,
        new To2DoneCodec().encoder(),
        cipher,
        encryptedMessageCodec);

    httpResponse = httpPost(
        uri.resolve(HttpPath.of(to2Done)),
        httpResponse.headers().firstValue(HttpUtil.AUTHORIZATION).orElse(null),
        cryptText);
    To2Done2 done2 = decipherAndDecode(
        httpResponse.body(),
        encryptedMessageCodec,
        cipher,
        new To2Done2Codec().decoder());

    if (!Objects.equals(n7, done2.getN7())) {
      throw new ProtocolException(new Error(
          ErrorCode.MessageRefused, setupDevice.getType(),
          "TO2.Done2.n7 invalid"));
    }

    return result;
  }
}
