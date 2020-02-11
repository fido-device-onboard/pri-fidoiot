// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import org.bouncycastle.crypto.DataLengthException;
import org.sdo.cri.AsymmetricKeyExchange.Owner;
import org.sdo.cri.EpidLib.EpidStatus;
import org.sdo.cri.EpidLib.HashAlg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OwnerService implements ProtocolService, Serializable {

  private static final int TWO_K = 1024 * 2;
  private static final int THREE_K = 1024 * 3;

  private ProtocolCipher myCipher = null;
  private CipherType myCipherType = null;
  private transient BiConsumer<OwnershipVoucher, Error> myDeviceErrorHandler = this::handleError;
  private EncryptedMessageCodec myEncryptedMessageCodec = null;
  private URL myEpidServiceUrl = EpidConstants.onlineEpidUrlDefault;
  private UUID myG3 = null;
  private transient Function<OwnershipVoucher, UUID> myG3Function = this::defaultG3;
  private transient HttpClient myHttpClient = HttpClient.newBuilder().build();
  private boolean myIsDone = false;
  private KeyExchange myKeyExchange = null;
  private Nonce myN6 = null;
  private Nonce myN7 = null;
  private Integer myNn = null;
  private OwnershipVoucher113 myOwnershipVoucher = null;
  private transient ObjectStorage<UUID, OwnershipVoucher> myOwnershipVoucherStorage = null;
  private RendezvousInfo myR3 = null;
  private transient Function<OwnershipVoucher, RendezvousInfo> myR3Function = this::defaultR3;
  private transient SecureRandom mySecureRandom = new SecureRandom();
  private Iterator<Supplier<ServiceInfo>> myServiceInfoIterator = null;
  private transient Collection<ServiceInfoModule> myServiceInfoModules = List.of();
  private transient Function<KeyType, KeyPair> myKeysProvider = null;

  private EpidLib buildEpidLib() throws URISyntaxException {
    return new EpidLib(
        new EpidOnlineMaterial(myEpidServiceUrl.toURI(), myHttpClient),
        new EpidOnlineVerifier(myEpidServiceUrl.toURI(), myHttpClient));
  }

  private <T> T decipherAndDecode(ProtocolMessage in, ProtocolDecoder<T> decoder) {
    if (null == myCipher || null == myEncryptedMessageCodec) {
      throw fail(
          ErrorCode.MessageRefused,
          in.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    try {
      final CipherText113a ct = myEncryptedMessageCodec.decode(in.getBody());
      final byte[] plainAscii = myCipher.decipher(ct);
      final CharBuffer plainText = US_ASCII.decode(ByteBuffer.wrap(plainAscii));
      logger().debug(MessageFormat.format(
          loadResourceBundle().getString("INFO_POST_DECIPHER"), plainText.toString()));
      return decoder.decode(plainText);

    } catch (ParseException e) {
      throw fail(
          ErrorCode.SyntaxError,
          in.getType(),
          loadResourceBundle().getString("WARN_MESSAGE_REFUSED"));

    } catch (HmacVerificationException e) {
      throw fail(
          ErrorCode.MessageRefused,
          in.getType(),
          loadResourceBundle().getString("WARN_MESSAGE_REFUSED"));

    } catch (InvalidKeyException | IOException e) {
      // these errors shouldn't occur, and are probably symptoms of a bug.
      throw fail(ErrorCode.InternalError, in.getType(), e.getMessage());
    }
  }

  private <T> T decodeMessageBody(
      Version version,
      MessageType messageType,
      CharSequence body,
      ProtocolDecoder<T> decoder)
      throws ProtocolException {

    try {
      return decoder.decode(CharBuffer.wrap(body));
    } catch (Exception e) {
      final String format = loadResourceBundle().getString("ERR_DECODE");
      throw fail(
          ErrorCode.MessageRefused,
          messageType,
          MessageFormat.format(format, version, messageType, body));
    }
  }

  private <T> T decodeMessageBody(ProtocolMessage in, ProtocolDecoder<T> decoder)
      throws ProtocolException {

    return decodeMessageBody(in.getVersion(), in.getType(), in.getBody(), decoder);
  }

  private UUID defaultG3(OwnershipVoucher voucher) {
    return null;
  }

  private RendezvousInfo defaultR3(OwnershipVoucher voucher) {
    return null;
  }

  private EncodedProtocolMessage encipherAndEncode(EncodedProtocolMessage in) {

    if (null == myCipher || null == myEncryptedMessageCodec) {
      throw fail(
          ErrorCode.MessageRefused,
          in.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    logger().debug(MessageFormat.format(
        loadResourceBundle().getString("INFO_PRE_ENCIPHER"), in.getBody()));

    final String body;
    try {
      final CipherText113a cipherText = myCipher.encipher(in.getBody().getBytes(US_ASCII));
      body = myEncryptedMessageCodec.encode(cipherText);
    } catch (InvalidKeyException e) {
      throw fail(
          ErrorCode.InternalError,
          in.getType(),
          MessageFormat.format(
              loadResourceBundle().getString("ERR_ENCODING_FAILURE"), e.getMessage()));
    }

    return EncodedProtocolMessage.getInstance(in.getVersion(), in.getType(), body);
  }

  private <T> String encodeToString(T o, ProtocolEncoder<T> encoder) {
    StringWriter w = new StringWriter();
    try {
      encoder.encode(w, o);
    } catch (IOException e) {
      // we should never see IOExceptions when writing to strings
      throw fail(ErrorCode.InternalError, MessageType.ERROR, e.getMessage());
    }
    return w.toString();
  }

  private ProtocolException fail(ErrorCode ec, MessageType cause, String message) {
    finish();
    return new ProtocolException(new Error(ec, cause, message));
  }

  private void finish() {
    myIsDone = true;
  }

  private void handleError(OwnershipVoucher voucher, Error error) {
    logger().error(MessageFormat.format(
        loadResourceBundle().getString("ERR_DEVICE_ERROR"),
        null != voucher ? voucher.getUuid() : null,
        error.getEc(),
        error.getEm()));
  }

  private Nonce initN6() {
    myN6 = new Nonce(mySecureRandom);
    return myN6;
  }

  @Override
  public boolean isDone() {
    return myIsDone;
  }

  @Override
  public boolean isHello(ProtocolMessage message) {
    return Version.VERSION_1_13 == message.getVersion()
        && MessageType.TO2_HELLO_DEVICE == message.getType();
  }

  @Override
  public ProtocolMessage next(ProtocolMessage in) throws ProtocolException {

    if (myIsDone) {
      throw fail(ErrorCode.MessageRefused,
          in.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    if (Version.VERSION_1_13 != in.getVersion()) {
      final String format = loadResourceBundle().getString("INVALID_VERSION");
      throw fail(
          ErrorCode.MessageRefused,
          in.getType(),
          MessageFormat.format(format, in.getVersion()));
    }

    switch (in.getType()) {
      case TO2_HELLO_DEVICE:
        return next(decodeMessageBody(in, new To2HelloDeviceCodec().decoder()::apply));

      case TO2_GET_OP_NEXT_ENTRY:
        return next(decodeMessageBody(in, new To2GetOpNextEntryCodec().decoder()::apply));

      case TO2_PROVE_DEVICE:
        final SignatureBlock sb = decodeMessageBody(in, new SignatureBlockCodec.Decoder(null));
        final To2ProveDevice to2ProveDevice = decodeMessageBody(
            in.getVersion(),
            in.getType(),
            sb.getBo(),
            new To2ProveDeviceCodec().decoder()::apply);
        return encipherAndEncode(next(sb, to2ProveDevice));

      case TO2_NEXT_DEVICE_SERVICE_INFO:
        return encipherAndEncode(
            next(decipherAndDecode(in, new To2NextDeviceServiceInfoCodec().decoder()::apply)));

      case TO2_GET_NEXT_OWNER_SERVICE_INFO:
        return encipherAndEncode(
            next(decipherAndDecode(in, new To2GetNextOwnerServiceInfoCodec().decoder()::apply)));

      case TO2_DONE:
        return encipherAndEncode(
            next(decipherAndDecode(in, new To2DoneCodec().decoder()::apply)));

      case ERROR:
        return next(decodeMessageBody(in, new ErrorCodec().decoder()::apply));

      default:
        final String format = loadResourceBundle().getString("INVALID_MESSAGE_TYPE");
        throw fail(
            ErrorCode.MessageRefused,
            in.getType(),
            MessageFormat.format(format, in.getType()));
    }
  }

  private EncodedProtocolMessage next(To2HelloDevice to2HelloDevice) throws ProtocolException {

    // This is the first time we see the voucher UUID, so try and load the voucher
    final UUID g2 = to2HelloDevice.getG2();
    if (null != myOwnershipVoucherStorage) {
      var ov = myOwnershipVoucherStorage.load(g2).orElseThrow(() -> fail(
          ErrorCode.ResourceNotFound,
          to2HelloDevice.getType(),
          MessageFormat.format(loadResourceBundle().getString("ERR_PROXY_NOT_FOUND"), g2)));
      if (ov instanceof OwnershipVoucher113) {
        myOwnershipVoucher = (OwnershipVoucher113) ov;
      } else {
        throw fail(
            ErrorCode.InvalidOwnershipProxy,
            to2HelloDevice.getType(),
            loadResourceBundle().getString("ERR_INVALID_VOUCHER"));
      }
    } else {
      throw fail(
          ErrorCode.ResourceNotFound,
          to2HelloDevice.getType(),
          loadResourceBundle().getString("ERR_PROXY_STORAGE_NULL"));
    }

    // Initialize the key exchange, which won't be performed until step 44 but we must pass the
    // first exchange message here, as xA.
    switch (to2HelloDevice.getKx()) {
      case ASYMKEX:
        myKeyExchange = buildAsym2KKeyExchange();
        break;

      case ASYMKEX3072:
        myKeyExchange = buildAsym3KKeyExchange();
        break;

      case DHKEXid14:
        myKeyExchange = new DiffieHellmanKeyExchange.Group14(mySecureRandom);
        break;

      case DHKEXid15:
        myKeyExchange = new DiffieHellmanKeyExchange.Group15(mySecureRandom);
        break;

      case ECDH:
        myKeyExchange = new EcdhKeyExchange.P256.Owner(mySecureRandom);
        break;

      case ECDH384:
        myKeyExchange = new EcdhKeyExchange.P384.Owner(mySecureRandom);
        break;

      default:
        throw fail(
            ErrorCode.MessageRefused,
            to2HelloDevice.getType(),
            loadResourceBundle().getString("ERR_INVALID_KEY_EXCHANGE_TYPE"));
    }

    // remember the requested cipher type, we'll need it in message 44.
    myCipherType = to2HelloDevice.getCs();

    final To2ProveOpHdr to2ProveOpHdr;
    try {
      to2ProveOpHdr = new To2ProveOpHdr(
          myOwnershipVoucher.getEn().size(),
          myOwnershipVoucher.getOh(),
          myOwnershipVoucher.getHmac(),
          to2HelloDevice.getN5(),
          initN6(),
          new SigInfoResponder(buildEpidLib()).apply(to2HelloDevice.getEa()),
          myKeyExchange.getMessage());
    } catch (Exception e) {
      throw fail(
          ErrorCode.InternalError,
          to2HelloDevice.getType(),
          e.getMessage());
    }

    if (null == myKeysProvider) {
      throw fail(ErrorCode.MessageRefused,
          to2HelloDevice.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    KeyType voucherKeyType = Keys.toType(myOwnershipVoucher.getOh().getPk());
    final SignatureBlock signatureBlock;
    try (KeyPairCloser keys = new KeyPairCloser(myKeysProvider.apply(voucherKeyType))) {
      String bo = encodeToString(to2ProveOpHdr, new To2ProveOpHdrCodec().encoder()::apply);
      signatureBlock = new SignatureBlock(
          bo,
          myOwnershipVoucher.getCurrentOwnerKey(),
          Signatures.sign(bo, keys.getPrivate()));

    } catch (GeneralSecurityException | IOException e) {
      throw fail(ErrorCode.InternalError, to2HelloDevice.getType(), e.getMessage());
    }

    final String responseBody = encodeToString(
        signatureBlock,
        new SignatureBlockCodec.Encoder(
            new PublicKeyCodec.Encoder(myOwnershipVoucher.getOh().getPe())));

    return EncodedProtocolMessage.getInstance(
        to2ProveOpHdr.getVersion(), to2ProveOpHdr.getType(), responseBody);
  }

  private EncodedProtocolMessage next(To2GetOpNextEntry to2GetOpNextEntry)
      throws ProtocolException {

    final Integer enn = to2GetOpNextEntry.getEnn();
    final To2OpNextEntry to2OpNextEntry =
        new To2OpNextEntry(enn, myOwnershipVoucher.getEn().get(enn));
    final String responseBody = encodeToString(
        to2OpNextEntry,
        new To2OpNextEntryCodec.Encoder(
            new SignatureBlockCodec.Encoder(
                new PublicKeyCodec.Encoder(myOwnershipVoucher.getOh().getPe()))));

    return EncodedProtocolMessage.getInstance(
        to2OpNextEntry.getVersion(), to2OpNextEntry.getType(), responseBody);
  }

  private EncodedProtocolMessage next(SignatureBlock signatureBlock,
      To2ProveDevice to2ProveDevice)
      throws ProtocolException {

    PublicKey devicePk = signatureBlock.getPk();

    // 1.13a 5.6.6 non-epid device keys must result in a null TO2.ProveDevice.pk
    if (null == devicePk) {
      CertPath certPath = myOwnershipVoucher.getDc();

      if (null != certPath) {
        List<? extends Certificate> certs = certPath.getCertificates();

        if (!certs.isEmpty()) {
          devicePk = certs.get(0).getPublicKey();

        } else { // certpath is empty
          throw fail(
              ErrorCode.MessageRefused,
              to2ProveDevice.getType(),
              loadResourceBundle().getString("ERR_VOUCHER_DC_EMPTY"));
        }

      } else { // certpath is null
        throw fail(
            ErrorCode.MessageRefused,
            to2ProveDevice.getType(),
            loadResourceBundle().getString("ERR_VOUCHER_DC_AND_PK_NULL"));
      }
    }

    // Testing N6 proves that we're in the right state and that the device isn't repeating itself
    // EPID signatures need a valid N6 so we must do this first.
    if (null == myN6) {
      throw fail(
          ErrorCode.MessageRefused,
          to2ProveDevice.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));

    } else if (!Objects.equals(myN6, to2ProveDevice.getN6())) {
      throw fail(
          ErrorCode.MessageRefused,
          to2ProveDevice.getType(),
          loadResourceBundle().getString("ERR_INVALID_NONCE"));
    }

    // Test the message signature.  If the key's provided, then it's an EPID signature.
    // Otherwise we have to get the verification key from the device cert chain
    // in the voucher header.
    final boolean isVerified;
    if (devicePk instanceof EpidKey) {
      try {
        final EpidLib epidLib = buildEpidLib();
        if (devicePk instanceof EpidKey10) {
          isVerified = (EpidStatus.kEpidNoErr.getValue() == epidLib.verify10Signature(
              devicePk.getEncoded(),
              signatureBlock.getSg(),
              Buffers.unwrap(US_ASCII.encode(signatureBlock.getBo())),
              myN6.getBytes(),
              to2ProveDevice.getAi()));
        } else if (devicePk instanceof EpidKey11) {
          isVerified = (EpidStatus.kEpidNoErr.getValue() == epidLib.verify11Signature(
              devicePk.getEncoded(),
              signatureBlock.getSg(),
              Buffers.unwrap(US_ASCII.encode(signatureBlock.getBo())),
              myN6.getBytes(),
              to2ProveDevice.getAi()));
        } else if (devicePk instanceof EpidKey20) {
          isVerified = (EpidStatus.kEpidNoErr.getValue() == epidLib.verify20Signature(
              devicePk.getEncoded(),
              HashAlg.KSHA256.getValue(),
              signatureBlock.getSg(),
              Buffers.unwrap(US_ASCII.encode(signatureBlock.getBo()))));
        } else {
          isVerified = false;
        }
      } catch (IOException | InterruptedException | URISyntaxException e) {
        throw fail(ErrorCode.InternalError, to2ProveDevice.getType(), e.getMessage());
      }
    } else {
      try {
        isVerified = Signatures.verify(signatureBlock.getBo(), signatureBlock.getSg(), devicePk);
      } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
        throw fail(ErrorCode.InternalError, to2ProveDevice.getType(), e.getMessage());
      }
    }

    if (!isVerified) {
      throw fail(
          ErrorCode.MessageRefused,
          to2ProveDevice.getType(),
          loadResourceBundle().getString("ERR_INVALID_SIGNATURE"));
    }

    // 1.13a 5.6.6 all subsequent messages (including our reply) are enciphered.
    // The key exchange which began in message 40 can now be completed.
    if (null == myKeyExchange || null == myCipherType) {
      throw fail(
          ErrorCode.MessageRefused,
          to2ProveDevice.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    try (Buffers.Eraser eraser =
        new Buffers.Eraser(myKeyExchange.generateSharedSecret(
          ByteBuffer.wrap(to2ProveDevice.getXb())))) {

      // Which SEK/SVK derivation we use depends on the crypto level the device is using,
      // and therefore on its crypto level.  We can deduce this by looking at KX.
      CryptoLevels.CryptoLevel cryptoLevel =
          CryptoLevels.keyExchangeTypeToCryptoLevel(myKeyExchange.getType());

      final SecretKey sek =
          cryptoLevel.getSekDerivationFunction().apply(Buffers.unwrap(eraser.getBuf()));
      switch (myCipherType.getMode()) {
        case CTR:
          myCipher = new CtrCipher(sek, mySecureRandom);
          break;
        case CBC:
          myCipher = new CbcCipher(sek, mySecureRandom);
          break;
        default:
          throw fail(
              ErrorCode.MessageRefused,
              to2ProveDevice.getType(),
              loadResourceBundle().getString("ERR_INVALID_CIPHER_TYPE"));
      }

      final SecretKey svk =
          cryptoLevel.getSvkDerivationFunction().apply(Buffers.unwrap(eraser.getBuf()));
      myEncryptedMessageCodec = new EncryptedMessageCodec(svk);

    } catch (DataLengthException | IllegalArgumentException e) {
      throw fail(
          ErrorCode.MessageRefused, to2ProveDevice.getType(), e.getMessage());

    } catch (GeneralSecurityException e) {
      throw fail(
          ErrorCode.InternalError, to2ProveDevice.getType(), e.getMessage());

    } finally {
      // invalidate the now-used key exchange so we don't use it again
      myKeyExchange = null;
      myCipherType = null;
    }

    // We'll need n7 after the device service-info loop
    myN7 = to2ProveDevice.getN7();

    // The presence of the mandatory SDO-DEV service info module means the device will always
    // send at least one service info message.  We should never see the corner case
    // of nn == 0.
    myNn = to2ProveDevice.getNn();
    if (myNn < 1) {
      throw fail(
          ErrorCode.MessageRefused,
          to2ProveDevice.getType(),
          loadResourceBundle().getString("ERR_UNEXPECTED_NN_0"));
    }

    final PreServiceInfo preServiceInfo = new PreServiceInfo();
    myServiceInfoModules.stream()
        .filter(o -> o instanceof PreServiceInfoMultiSource)
        .map(PreServiceInfoMultiSource.class::cast)
        .map(o -> o.getPreServiceInfo(myOwnershipVoucher.getUuid()))
        .forEach(preServiceInfo::addAll);
    final To2GetNextDeviceServiceInfo getNextDeviceServiceInfo =
        new To2GetNextDeviceServiceInfo(0, preServiceInfo);

    final String responseBody = encodeToString(
        getNextDeviceServiceInfo, new To2GetNextDeviceServiceInfoCodec().encoder()::apply);

    return EncodedProtocolMessage.getInstance(
        getNextDeviceServiceInfo.getVersion(), getNextDeviceServiceInfo.getType(), responseBody);
  }

  private EncodedProtocolMessage next(To2NextDeviceServiceInfo to2NextDeviceServiceInfo)
      throws ProtocolException {

    if (null == myOwnershipVoucher || null == myNn) {
      throw fail(
          ErrorCode.MessageRefused,
          to2NextDeviceServiceInfo.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    // dispatch the service info to interested modules
    for (Object serviceInfoObject : myServiceInfoModules) {

      if (serviceInfoObject instanceof ServiceInfoSink) {
        ServiceInfoSink sink = (ServiceInfoSink) serviceInfoObject;

        for (Entry<CharSequence, CharSequence> entry : to2NextDeviceServiceInfo.getDsi()) {
          sink.putServiceInfo(entry);
        }

      } else if (serviceInfoObject instanceof ServiceInfoMultiSink) {
        ServiceInfoMultiSink sink = (ServiceInfoMultiSink) serviceInfoObject;

        for (Entry<CharSequence, CharSequence> entry : to2NextDeviceServiceInfo.getDsi()) {
          sink.putServiceInfo(myOwnershipVoucher.getUuid(), entry);
        }
      }
    }

    final int nn = 1 + to2NextDeviceServiceInfo.getNn();
    if (nn < myNn) {
      // If the service info sequence isn't yet exhausted, request the next one
      final To2GetNextDeviceServiceInfo to2GetNextDeviceServiceInfo =
          new To2GetNextDeviceServiceInfo(nn, new PreServiceInfo());
      final String responseBody = encodeToString(
          to2GetNextDeviceServiceInfo, new To2GetNextDeviceServiceInfoCodec().encoder()::apply);

      return EncodedProtocolMessage.getInstance(
          to2GetNextDeviceServiceInfo.getVersion(),
          to2GetNextDeviceServiceInfo.getType(),
          responseBody);

    } else {
      // If the list is complete, move on to TO2.SetupDevice
      myNn = null; // we don't need this anymore

      if (null == myN7 || null == myOwnershipVoucher) {
        throw fail(
            ErrorCode.MessageRefused,
            to2NextDeviceServiceInfo.getType(),
            loadResourceBundle().getString("ERR_INVALID_STATE"));
      }

      myR3 = myR3Function.apply(myOwnershipVoucher);
      if (null == myR3) { // null means 'reuse previous'
        myR3 = myOwnershipVoucher.getOh().getR();
      }

      myG3 = myG3Function.apply(myOwnershipVoucher);
      if (null == myG3) {
        myG3 = myOwnershipVoucher.getUuid();
      }

      final String nohBody = encodeToString(
          new To2SetupDeviceNoh(myR3, myG3, myN7),
          new To2SetupDeviceNohCodec().encoder()::apply);
      KeyType voucherKeyType = Keys.toType(myOwnershipVoucher.getOh().getPk());
      final SignatureBlock noh;
      try (KeyPairCloser keys = new KeyPairCloser(myKeysProvider.apply(voucherKeyType))) {
        noh = new SignatureBlock(
            nohBody,
            myOwnershipVoucher.getCurrentOwnerKey(),
            Signatures.sign(nohBody, keys.getPrivate()));

      } catch (GeneralSecurityException | IOException e) {
        throw fail(ErrorCode.InternalError, to2NextDeviceServiceInfo.getType(), e.getMessage());
      }

      // Compute osinn, which requires knowing how many service info messages will be sent.
      // As of this writing, the only way to get this number is to count them.
      final List<ServiceInfoSource> serviceInfoSources = new ArrayList<>();
      final List<ServiceInfoMultiSource> serviceInfoMultiSources = new ArrayList<>();
      for (Object serviceInfoObject : myServiceInfoModules) {

        if (serviceInfoObject instanceof ServiceInfoSource) {
          serviceInfoSources.add((ServiceInfoSource) serviceInfoObject);

        } else if (serviceInfoObject instanceof ServiceInfoMultiSource) {
          serviceInfoMultiSources.add((ServiceInfoMultiSource) serviceInfoObject);
        }
      }

      final ServiceInfoMarshaller marshaller = new ServiceInfoMarshaller();
      marshaller.setSources(serviceInfoSources);
      marshaller.setMultiSources(serviceInfoMultiSources);
      Iterable<Supplier<ServiceInfo>> serviceInfos =
          marshaller.marshal(myOwnershipVoucher.getUuid());

      int osinn = 0;
      for (Supplier<?> s : serviceInfos) {
        osinn++;
      }
      myServiceInfoIterator = serviceInfos.iterator();

      final To2SetupDevice to2SetupDevice = new To2SetupDevice(osinn, noh);
      final String responseBody = encodeToString(
          to2SetupDevice,
          new To2SetupDeviceCodec.Encoder(
              new SignatureBlockCodec.Encoder(
                  new PublicKeyCodec.Encoder(myOwnershipVoucher.getOh().getPe()))));

      return EncodedProtocolMessage.getInstance(
          to2SetupDevice.getVersion(), to2SetupDevice.getType(), responseBody);
    }

  }

  private EncodedProtocolMessage next(To2GetNextOwnerServiceInfo to2GetNextOwnerServiceInfo)
      throws ProtocolException {

    if (null == myServiceInfoIterator || !myServiceInfoIterator.hasNext()) {
      throw fail(
          ErrorCode.MessageRefused,
          to2GetNextOwnerServiceInfo.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    final Supplier<ServiceInfo> serviceInfoSupplier = myServiceInfoIterator.next();
    if (null == serviceInfoSupplier) {
      throw fail(
          ErrorCode.InternalError,
          to2GetNextOwnerServiceInfo.getType(),
          loadResourceBundle().getString("ERR_NULL_SERVICE_INFO_SUPPLIER"));
    }

    final ServiceInfo serviceInfo = serviceInfoSupplier.get();
    if (null == serviceInfo) {
      throw fail(
          ErrorCode.InternalError,
          to2GetNextOwnerServiceInfo.getType(),
          loadResourceBundle().getString("ERR_NULL_SERVICE_INFO"));
    }

    final To2OwnerServiceInfo to2OwnerServiceInfo =
        new To2OwnerServiceInfo(to2GetNextOwnerServiceInfo.getNn(), serviceInfo);
    final String responseBody = encodeToString(
        to2OwnerServiceInfo, new To2OwnerServiceInfoCodec().encoder()::apply);

    return EncodedProtocolMessage.getInstance(
        to2OwnerServiceInfo.getVersion(), to2OwnerServiceInfo.getType(), responseBody);
  }

  private EncodedProtocolMessage next(To2Done to2Done) throws ProtocolException {

    // Make sure our state is valid
    if (null == myN6 || null == myN7) {
      throw fail(
          ErrorCode.MessageRefused,
          to2Done.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));

    }

    // Make sure N6 matches
    if (!Objects.equals(myN6, to2Done.getN6())) {
      throw fail(
          ErrorCode.MessageRefused,
          to2Done.getType(),
          loadResourceBundle().getString("ERR_INVALID_NONCE"));
    }

    if (null != myOwnershipVoucherStorage) {
      if (null == myG3 || null == myR3 || null == myOwnershipVoucher) {
        throw fail(
            ErrorCode.MessageRefused,
            to2Done.getType(),
            loadResourceBundle().getString("ERR_INVALID_STATE"));
      }

      // Check for the credential reuse protocol.  If we're reusing credentials,
      // don't overwrite existing vouchers
      boolean isReuseEnabled = false;
      HashMac deviceHmac = to2Done.getHmac();
      if (MacType.NONE == deviceHmac.getType()) {
        ByteBuffer macBuf = deviceHmac.getHash();
        if (1 == macBuf.remaining() && Objects.equals(US_ASCII.encode("="), macBuf)) {
          isReuseEnabled = true;
        }
      }

      if (isReuseEnabled) {
        logger().info(MessageFormat.format(
            loadResourceBundle().getString("INFO_CREDENTIAL_REUSE_DETECTED"), myG3));
      } else {
        final OwnershipVoucher113 generatedVoucher;
        try {
          generatedVoucher = new OwnershipVoucher113(
              new OwnershipVoucherHeader(
                  myOwnershipVoucher.getOh().getPe(),
                  myR3,
                  myG3,
                  myOwnershipVoucher.getOh().getD(),
                  myOwnershipVoucher.getCurrentOwnerKey(),
                  myOwnershipVoucher.getOh().getHdc()),
              to2Done.getHmac(),
              myOwnershipVoucher.getDc(),
              List.of());
        } catch (IOException e) {
          // This should not happen, as we've already parsed the ownership voucher successfully.
          throw fail(
              ErrorCode.InternalError,
              to2Done.getType(),
              e.getMessage());
        }
        myOwnershipVoucherStorage.store(generatedVoucher.getUuid(), generatedVoucher);
      }
    }

    final To2Done2 to2Done2 = new To2Done2(myN7);
    final String responseBody = encodeToString(to2Done2, new To2Done2Codec().encoder()::apply);
    finish();

    return EncodedProtocolMessage.getInstance(
        to2Done2.getVersion(), to2Done2.getType(), responseBody);
  }

  private EncodedProtocolMessage next(Error error) throws ProtocolException {
    myDeviceErrorHandler.accept(myOwnershipVoucher, error);
    return null;
  }

  private KeyExchange buildAsym2KKeyExchange() {
    KeyPair keys = myKeysProvider.apply(KeyType.RSA2048RESTR);
    if (null == keys) {
      // If we can't get a restricted (exponent = F4) key pair, we might still be able
      // to get a short unrestricted keypair
      keys = myKeysProvider.apply(KeyType.RSA_UR);
      if (null != keys) {
        // RSA_UR doesn't strictly define key length, so we have to check if it's the right length
        RSAPublicKey rsaKey = (RSAPublicKey) keys.getPublic();
        if (TWO_K != rsaKey.getModulus().bitLength()) {
          // no good, toss this result out
          keys = null;
        }
      }
    }

    if (null != keys) { // we found a usable keypair
      return new Owner(keys, mySecureRandom);
    } else {
      return null;
    }
  }

  private KeyExchange buildAsym3KKeyExchange() {
    KeyPair keys = myKeysProvider.apply(KeyType.RSA_UR);
    if (null != keys) {
      // RSA_UR doesn't strictly define key length, so we have to check if it's the right length
      RSAPublicKey rsaKey = (RSAPublicKey) keys.getPublic();
      if (THREE_K != rsaKey.getModulus().bitLength()) {
        // no good, toss this result out
        keys = null;
      }
    }

    if (null != keys) { // we found a usable keypair
      return new Owner(keys, mySecureRandom);
    } else {
      return null;
    }
  }

  private ResourceBundle loadResourceBundle() {
    return ResourceBundle.getBundle(getClass().getPackageName() + ".OwnerService");
  }

  private Logger logger() {
    return LoggerFactory.getLogger(getClass());
  }

  public void setDeviceErrorHandler(BiConsumer<OwnershipVoucher, Error> errorHandler) {
    myDeviceErrorHandler = Objects.requireNonNull(errorHandler);
  }

  public void setEpidServiceUrl(URL url) {
    myEpidServiceUrl = Objects.requireNonNull(url);
  }

  public void setG3Function(Function<OwnershipVoucher, UUID> g3Function) {
    myG3Function = Objects.requireNonNull(g3Function);
  }

  public void setHttpClient(HttpClient httpClient) {
    myHttpClient = Objects.requireNonNull(httpClient);
  }

  public void setOwnershipVoucherStorage(ObjectStorage<UUID, OwnershipVoucher> storage) {
    this.myOwnershipVoucherStorage = Objects.requireNonNull(storage);
  }

  public void setR3Function(Function<OwnershipVoucher, RendezvousInfo> r3Function) {
    myR3Function = Objects.requireNonNull(r3Function);
  }

  public void setSecureRandom(SecureRandom secureRandom) {
    this.mySecureRandom = Objects.requireNonNull(secureRandom);
  }

  public void setServiceInfoModules(Collection<ServiceInfoModule> serviceInfoModules) {
    myServiceInfoModules =
        Collections.unmodifiableList(List.copyOf(Objects.requireNonNull(serviceInfoModules)));
  }

  public void setKeysProvider(Function<KeyType, KeyPair> provider) {
    myKeysProvider = Objects.requireNonNull(provider);
  }
}
