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
import java.nio.CharBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

public class RendezvousDeviceService implements ProtocolService, Serializable {

  private URL myEpidServiceUrl = EpidConstants.onlineEpidUrlDefault;
  private transient HttpClient myHttpClient = HttpClient.newBuilder().build();
  private boolean myIsDone = false;
  private Nonce myN4 = null;
  private RedirectionEntry myRedirectionEntry = null;
  private transient ObjectStorage<UUID, PerishableRecord> myRedirectionMap;
  private transient SecureRandom mySecureRandom = new SecureRandom();

  private EpidLib buildEpidLib() throws URISyntaxException {
    return new EpidLib(
        new EpidOnlineMaterial(myEpidServiceUrl.toURI(), myHttpClient),
        new EpidOnlineVerifier(myEpidServiceUrl.toURI(), myHttpClient));
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

  @Override
  public boolean isDone() {
    return myIsDone;
  }

  @Override
  public boolean isHello(ProtocolMessage message) {
    return Version.VERSION_1_13 == message.getVersion()
        && MessageType.TO1_HELLO_SDO == message.getType();
  }

  @Override
  public ProtocolMessage next(ProtocolMessage in) throws ProtocolException {

    if (myIsDone) {
      throw fail(ErrorCode.MessageRefused,
          in.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    if (Version.VERSION_1_13 != in.getVersion()) {
      final String format = loadResourceBundle().getString("ERR_INVALID_VERSION");
      throw fail(
          ErrorCode.MessageRefused,
          in.getType(),
          MessageFormat.format(format, in.getVersion()));
    }

    switch (in.getType()) {
      case TO1_HELLO_SDO:
        return next(decodeMessageBody(in, new To1HelloSdoCodec().decoder()::apply));

      case TO1_PROVE_TO_SDO:
        final SignatureBlock signatureBlock;
        final To1ProveToSdo to1ProveToSdo;
        try {
          signatureBlock =
              new SignatureBlockCodec.Decoder(null).decode(CharBuffer.wrap(in.getBody()));
          to1ProveToSdo =
              new To1ProveToSdoCodec().decoder().apply(CharBuffer.wrap(signatureBlock.getBo()));
        } catch (IOException e) {
          throw fail(ErrorCode.MessageRefused,
              in.getType(),
              MessageFormat.format(
                  loadResourceBundle().getString("ERR_DECODE"),
                  in.getVersion(),
                  in.getType(),
                  in.getBody()));
        }
        return next(signatureBlock, to1ProveToSdo);

      default:
        final String format = loadResourceBundle().getString("ERR_INVALID_MESSAGE_TYPE");
        throw fail(
            ErrorCode.MessageRefused,
            in.getType(),
            MessageFormat.format(format, in.getType()));
    }
  }

  private EncodedProtocolMessage next(To1HelloSdo to1HelloSdo) {

    // If the device's owner has completed TO0 recently, there will be a record
    // for this g2 in our redirect map.
    final RedirectionEntry redirectionEntry = lookupRedirect(to1HelloSdo.getG2());
    if (null == redirectionEntry) {
      throw fail(ErrorCode.ResourceNotFound,
          to1HelloSdo.getType(),
          MessageFormat.format(
              loadResourceBundle().getString("ERR_REDIRECT_NOT_FOUND"), to1HelloSdo.getG2()));
    }

    myRedirectionEntry = redirectionEntry;
    myN4 = new Nonce(mySecureRandom);

    final To1HelloSdoAck to1HelloSdoAck;
    try {
      to1HelloSdoAck = new To1HelloSdoAck(myN4,
          new SigInfoResponder(buildEpidLib()).apply(to1HelloSdo.getEa()));
    } catch (Exception e) {
      throw fail(ErrorCode.InternalError, to1HelloSdo.getType(), e.getMessage());
    }

    final String responseBody =
        encodeToString(to1HelloSdoAck, new To1HelloSdoAckCodec().encoder()::apply);

    return EncodedProtocolMessage.getInstance(
        to1HelloSdoAck.getVersion(), to1HelloSdoAck.getType(), responseBody);
  }

  private EncodedProtocolMessage next(
      SignatureBlock signatureBlock, To1ProveToSdo to1ProveToSdo) {

    if (null == myN4 || null == myRedirectionEntry) {
      throw fail(ErrorCode.MessageRefused,
          to1ProveToSdo.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    // NULL pk means the device key is ECDSA, and we must get it from the voucher header DC.
    PublicKey pk = signatureBlock.getPk();
    if (null == pk) {
      pk = myRedirectionEntry.getDevicePk();
    }

    if (null == pk) {
      throw fail(
          ErrorCode.MessageRefused,
          MessageType.TO1_PROVE_TO_SDO,
          loadResourceBundle().getString("ERR_NULL_PK_IN_PROVE"));
    }

    final boolean isVerified;
    try {
      if (pk instanceof EpidKey10) {
        isVerified = (EpidLib.EpidStatus.kEpidNoErr.getValue() == buildEpidLib().verify10Signature(
            pk.getEncoded(),
            signatureBlock.getSg(),
            Buffers.unwrap(US_ASCII.encode(signatureBlock.getBo())),
            to1ProveToSdo.getN4().getBytes(),
            to1ProveToSdo.getAi()));
      } else if (pk instanceof EpidKey11) {
        isVerified = (EpidLib.EpidStatus.kEpidNoErr.getValue() == buildEpidLib().verify11Signature(
            pk.getEncoded(),
            signatureBlock.getSg(),
            Buffers.unwrap(US_ASCII.encode(signatureBlock.getBo())),
            to1ProveToSdo.getN4().getBytes(),
            to1ProveToSdo.getAi()));
      } else if (pk instanceof EpidKey20) {
        isVerified = (EpidLib.EpidStatus.kEpidNoErr.getValue() == buildEpidLib().verify20Signature(
            pk.getEncoded(),
            EpidLib.HashAlg.KSHA256.getValue(),
            signatureBlock.getSg(),
            Buffers.unwrap(US_ASCII.encode(signatureBlock.getBo()))));
      } else {
        isVerified = Signatures.verify(signatureBlock.getBo(), signatureBlock.getSg(), pk);
      }
    } catch (GeneralSecurityException | IOException | URISyntaxException e) {
      throw fail(ErrorCode.InternalError, to1ProveToSdo.getType(), e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw fail(ErrorCode.InternalError, to1ProveToSdo.getType(), e.getMessage());
    }

    if (!isVerified) {
      throw fail(ErrorCode.MessageRefused,
          MessageType.TO1_PROVE_TO_SDO,
          loadResourceBundle().getString("ERR_INVALID_SIGNATURE"));
    }

    if (!Objects.equals(myN4, to1ProveToSdo.getN4())) {
      throw fail(ErrorCode.MessageRefused,
          to1ProveToSdo.getType(),
          loadResourceBundle().getString("ERR_INVALID_NONCE"));
    }

    finish();

    return EncodedProtocolMessage.getInstance(
        Version.VERSION_1_13, MessageType.TO1_SDO_REDIRECT, myRedirectionEntry.getRedirect());
  }

  private RedirectionEntry lookupRedirect(UUID uuid) {
    if (null != myRedirectionMap) {
      Optional<PerishableRecord> o = myRedirectionMap.load(uuid);
      if (o.isPresent()) {
        Perishable p = o.get();
        if (p instanceof RedirectionEntry) {
          return (RedirectionEntry) p;
        }
      }
    }
    return null;
  }

  private ResourceBundle loadResourceBundle() {
    return ResourceBundle.getBundle(getClass().getPackageName() + ".RendezvousService");
  }

  public void setEpidServiceUrl(URL url) {
    myEpidServiceUrl = Objects.requireNonNull(url);
  }

  public void setHttpClient(HttpClient httpClient) {
    myHttpClient = Objects.requireNonNull(httpClient);
  }

  public void setRedirectionMap(ObjectStorage<UUID, PerishableRecord> redirectionMap) {
    myRedirectionMap = Objects.requireNonNull(redirectionMap);
  }

  public void setSecureRandom(SecureRandom secureRandom) {
    this.mySecureRandom = Objects.requireNonNull(secureRandom);
  }
}
