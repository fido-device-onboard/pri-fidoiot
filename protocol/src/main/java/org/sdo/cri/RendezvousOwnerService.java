// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.BiFunction;

public class RendezvousOwnerService implements ProtocolService, Serializable {

  private boolean myIsDone = false;
  private Nonce myN3 = null;
  private transient ObjectStorage<UUID, PerishableRecord> myRedirectionMap = null;
  private transient SecureRandom mySecureRandom = new SecureRandom();
  private transient BiFunction<OwnershipVoucher, Duration, Duration> myWaitSecondsResponder =
      (voucher, request) -> request;

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
        && MessageType.TO0_HELLO == message.getType();
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
      case TO0_HELLO:
        return next(decodeMessageBody(in, new To0HelloCodec().decoder()::apply));

      case TO0_OWNER_SIGN:
        To0OwnerSignCodec.Decoder decoder = new To0OwnerSignCodec.Decoder();
        To0OwnerSign to0OwnerSign = decodeMessageBody(in, decoder);
        return next(to0OwnerSign, decoder.getLastTo1d().toString());

      default:
        final String format = loadResourceBundle().getString("ERR_INVALID_MESSAGE_TYPE");
        throw fail(
            ErrorCode.MessageRefused,
            in.getType(),
            MessageFormat.format(format, in.getType()));
    }
  }

  private EncodedProtocolMessage next(To0Hello to0Hello) {

    myN3 = new Nonce(mySecureRandom);

    final To0HelloAck helloAck = new To0HelloAck(myN3);
    final String responseBody = encodeToString(helloAck, new To0HelloAckCodec().encoder()::apply);

    return EncodedProtocolMessage.getInstance(
        helloAck.getVersion(), helloAck.getType(), responseBody);
  }

  private EncodedProtocolMessage next(To0OwnerSign to0OwnerSign, String to1d) {

    if (null == myN3) {
      throw fail(ErrorCode.MessageRefused,
          to0OwnerSign.getType(),
          loadResourceBundle().getString("ERR_INVALID_STATE"));
    }

    if (!Objects.equals(myN3, to0OwnerSign.getTo0d().getN3())) {
      throw fail(ErrorCode.MessageRefused,
          to0OwnerSign.getType(),
          loadResourceBundle().getString("ERR_INVALID_NONCE"));
    }

    OwnershipVoucher113 voucher = to0OwnerSign.getTo0d().getOp();
    List<SignatureBlock> en = voucher.getEn();

    // This message must be signed by the key at the end of the proxy's "en" chain.
    // If the chain is absent, the initial owner key in the header is used.
    PublicKey pk = voucher.getOh().getPk();

    if (0 < en.size()) {
      SignatureBlock signedEntry = en.get(en.size() - 1);
      final OwnershipVoucherEntry entry;
      try {
        entry =
            new OwnershipVoucherEntryCodec.Decoder().decode(CharBuffer.wrap(signedEntry.getBo()));
      } catch (IOException e) {
        // This shouldn't happen, as the voucher already correctly decoded before this
        // method was called.
        throw fail(ErrorCode.InternalError, to0OwnerSign.getType(), e.getMessage());
      }
      pk = entry.getPk();
    }

    final boolean isVerified;
    try {
      isVerified = Signatures.verify(
          to0OwnerSign.getTo1d().getBo(), to0OwnerSign.getTo1d().getSg(), pk);
    } catch (GeneralSecurityException e) {
      throw fail(ErrorCode.InternalError, to0OwnerSign.getType(), e.getMessage());
    }

    if (!isVerified) {
      throw fail(ErrorCode.MessageRefused,
          to0OwnerSign.getType(),
          loadResourceBundle().getString("ERR_INVALID_SIGNATURE"));
    }

    final Duration ws =
        myWaitSecondsResponder.apply(voucher, to0OwnerSign.getTo0d().getWs());

    PublicKey devicePk = null;
    if (null != to0OwnerSign.getTo0d().getOp().getDc()) {
      final List<? extends Certificate> certificates =
          to0OwnerSign.getTo0d().getOp().getDc().getCertificates();

      if (!certificates.isEmpty()) {
        devicePk = certificates.get(0).getPublicKey();
      }
    }

    final To0AcceptOwner to0AcceptOwner = new To0AcceptOwner(ws);
    final String responseBody =
        encodeToString(to0AcceptOwner, new To0AcceptOwnerCodec().encoder()::apply);

    if (null != myRedirectionMap) {
      myRedirectionMap.store(
          to0OwnerSign.getTo0d().getOp().getUuid(),
          new RedirectionEntry(devicePk, to1d, Instant.now().plus(ws)));
    }

    finish();

    return EncodedProtocolMessage.getInstance(
        to0AcceptOwner.getVersion(), to0AcceptOwner.getType(), responseBody);
  }

  private ResourceBundle loadResourceBundle() {
    return ResourceBundle.getBundle(getClass().getPackageName() + ".RendezvousService");
  }

  public void setRedirectionMap(ObjectStorage<UUID, PerishableRecord> redirectionMap) {
    myRedirectionMap = Objects.requireNonNull(redirectionMap);
  }

  public void setSecureRandom(SecureRandom secureRandom) {
    this.mySecureRandom = Objects.requireNonNull(secureRandom);
  }

  public void setWaitSecondsResponder(
      BiFunction<OwnershipVoucher, Duration, Duration> waitSecondsResponder) {
    myWaitSecondsResponder = Objects.requireNonNull(waitSecondsResponder);
  }
}
