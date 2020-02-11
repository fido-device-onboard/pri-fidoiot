// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.crypto.KeyAgreement;
import org.bouncycastle.jce.ECNamedCurveTable;

/**
 * Implement the ECDH key exchange, per SDO protocol spec 1.12m 2.5.5.3.
 */
abstract class EcdhKeyExchange implements KeyExchange, Serializable {

  // From protocol spec 1.12m, 2.5.5.3
  //
  // The Device and Owner each choose random numbers (Owner: a, Device: b)
  // and encode these numbers into exchanged parameters
  // A = (G x , G y )*a mod p, and
  // B = (G x , G y )*b mod p.
  // A and B are points, and have components (A x , A y ) and (B x , B y ),
  // respectively, with bit lengths same as (G x , G y ).
  //
  //        PROGRAMMER'S NOTE:
  //
  //        The above describes the generation of ECDSA key pairs,
  //        with 'a' & 'b' corresponding to private keys (commonly called 'd')
  //        and 'A' & 'B' corresponding to public keys
  //        (commonly called 'Q', or 'W' by the JCE javadoc).
  //
  // The Device and Owner each choose a random number (as per table above),
  // to be supplied with their public keys, respectively DeviceRandom, and OwnerRandom.
  //
  //        PROGRAMMER'S NOTE:
  //
  //        The table referred to is:
  //
  //                          SDO1.0 & SDO1.1              | Future Crypto
  //                         -----------------+------------+------------+----------
  //                          ECC Curve       | Randoms    | ECC Curve  | Randoms
  //        ----------------+-----------------+------------+------------+----------
  //         ECDH KEX       | NIST P-256      | 128 bits   | NIST P-384 | 384 bits
  //
  //
  // The Owner sends
  // ByteArray[blen(A x ), A x , blen(A y ), A y , blen(OwnerRandom), OwnerRandom]
  // to the Device as parameter TO2.ProveOPHdr.bo.xA.
  //
  // The Device sends
  // ByteArray[blen(B x ), B x , blen(B y ), B y , blen(DeviceRandom),DeviceRandom]
  // to the Owner as parameter TO2.ProveDevice.bo.xB.
  //
  // The Owner computes shared secret
  // Sh = (B*a mod p), with components (Sh x , Sh y ).
  //
  // The Device computes shared secret
  // Sh = (A*b mod p), with components (Sh x , Sh y ).
  //
  //        PROGRAMMER'S NOTE:
  //
  //        This text describes a standard ECDH key exchange, with 'a' substituting for the
  //        traditional d(a), 'A' for the traditional Q(a), and so on.
  //
  // The shared secret ShSe is formed as:
  // Sh x ||DeviceRandom||OwnerRandom
  // (Note that Sh y is not used to construct ShSe).

  private static final String ECDH = "ECDH";
  private static final String ECDSA = "EC";
  private final String curve;
  private final int kexRandomSize;
  private final SecureRandom secureRandom;
  private final KeyExchangeType type;
  private State state = null;

  private EcdhKeyExchange(
      KeyExchangeType type, String curve, int kexRandomSize, SecureRandom secureRandom) {

    this.type = type;
    this.curve = curve;
    this.kexRandomSize = kexRandomSize;
    this.secureRandom = secureRandom;
  }

  // Check if the blen() headers add up correctly.
  private static boolean verifyBlen(final ByteBuffer buf) {

    while (buf.hasRemaining()) {

      final int blen;
      try {
        blen = Blen.decode(buf);

      } catch (BufferUnderflowException ignored) {
        return false;
      }

      if (0 <= blen && blen <= buf.remaining()) {
        buf.position(buf.position() + blen);

      } else {
        return false;

      }
    }

    return true;
  }

  @Override
  public ByteBuffer generateSharedSecret(ByteBuffer message) throws
      InvalidAlgorithmParameterException,
      InvalidKeyException,
      InvalidKeySpecException,
      NoSuchAlgorithmException {

    State state = Objects.requireNonNull(getState());
    List<byte[]> values = new ArrayList<>();

    if (!verifyBlen(message.duplicate())) {
      throw new IllegalArgumentException("blen() does not fit buffer");
    }

    while (message.hasRemaining()) {
      final byte[] bytes = new byte[Blen.decode(message)];
      message.get(bytes);
      values.add(bytes);
    }

    byte[] theirX = values.remove(0);
    byte[] theirY = values.remove(0);
    byte[] theirRandom = values.remove(0);

    ECPrivateKey myPrivateKey = (ECPrivateKey) state.getMyKeyPair().getPrivate();
    ECPoint w = new ECPoint(new BigInteger(1, theirX), new BigInteger(1, theirY));
    ECPublicKeySpec keySpec = new ECPublicKeySpec(w, myPrivateKey.getParams());

    KeyFactory keyFactory = KeyFactory.getInstance(ECDSA, BouncyCastleLoader.load());
    ECPublicKey theirPublicKey = (ECPublicKey) keyFactory.generatePublic(keySpec);

    KeyAgreement keyAgreement = KeyAgreement.getInstance(ECDH, BouncyCastleLoader.load());
    keyAgreement.init(myPrivateKey);
    keyAgreement.doPhase(theirPublicKey, true);
    byte[] secret = keyAgreement.generateSecret();

    return generateSharedSecret(secret, state.getMyRandom(), theirRandom);
  }

  // Assemble the final shared secret (ShSe).
  //
  // The assembly is asymmetric, with an A side and a B side, so defer it to subclasses.
  protected abstract ByteBuffer generateSharedSecret(byte[] shx, byte[] myRnd, byte[] theirRnd);

  @Override
  public ByteBuffer getMessage() throws
      InvalidAlgorithmParameterException,
      NoSuchAlgorithmException,
      IOException {

    State state = Objects.requireNonNull(getState());
    ECPublicKey myPublicKey = (ECPublicKey) (state).getMyKeyPair().getPublic();
    int keySize = Keys.sizeInBytes(myPublicKey);

    // ...ByteArray[blen(A x ), A x , blen(A y ), A y , blen(random), random]...
    List<byte[]> elements = Arrays.asList(
        BigIntegers.toByteArray(myPublicKey.getW().getAffineX(), keySize),
        BigIntegers.toByteArray(myPublicKey.getW().getAffineY(), keySize),
        state.getMyRandom());

    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();

    try (WritableByteChannel outChannel = Channels.newChannel(outBytes)) {

      for (byte[] element : elements) {
        outChannel.write(ByteBuffer.wrap(Blen.encode(element.length)));
        outChannel.write(ByteBuffer.wrap(element));
      }
    }

    return ByteBuffer.wrap(outBytes.toByteArray());
  }

  private SecureRandom getSecureRandom() {
    return secureRandom;
  }

  @Override
  public KeyExchangeType getType() {
    return type;
  }

  private String getCurve() {
    return curve;
  }

  private int getKexRandomSize() {
    return kexRandomSize;
  }

  private State getState() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
    if (null == state) {
      init();
    }
    return state;
  }

  private void setState(State state) {
    this.state = state;
  }

  // Initialize the key exchange.
  //
  // Installs JCE providers and builds artifacts needed for the key exchange.
  // This is a heavy lift, so it's done as lazily as possible.
  private void init() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {

    final KeyPairGenerator gen;
    gen = KeyPairGenerator.getInstance(ECDSA, BouncyCastleLoader.load());
    gen.initialize(
        ECNamedCurveTable.getParameterSpec(getCurve()), getSecureRandom());

    KeyPair myKeyPair = gen.generateKeyPair();

    byte[] myRandom = new byte[getKexRandomSize()];
    getSecureRandom().nextBytes(myRandom);

    State state = new State(myKeyPair, myRandom);
    setState(state);
  }

  private static class Blen {

    static int decode(ByteBuffer buf) {
      byte[] blen = new byte[length()];
      buf.get(blen);
      return ByteBuffer.wrap(blen).asShortBuffer().get();
    }

    static byte[] encode(int len) {
      byte[] blen = new byte[length()];
      ByteBuffer.wrap(blen).asShortBuffer().put((short) (len & 0xffff));
      return blen;
    }

    static int length() {
      // Per SDO 1.12 2.5.5.3 + CR038, blen encodes in two bytes.
      return 2;
    }
  }

  /**
   * Implement the B (device) side of the ECDH key exchange, per SDO protocol spec 1.12m 2.5.5.3.
   */
  private abstract static class Device extends EcdhKeyExchange {

    Device(KeyExchangeType type, String curve, int kexRandomSize,
        SecureRandom secureRandom) {
      super(type, curve, kexRandomSize, secureRandom);
    }

    @Override
    protected ByteBuffer generateSharedSecret(byte[] shx, byte[] myRnd, byte[] theirRnd) {

      ByteBuffer shSe = ByteBuffer.allocate(shx.length + myRnd.length + theirRnd.length);

      shSe.put(shx);
      shSe.put(myRnd);
      shSe.put(theirRnd);
      shSe.flip();

      return shSe;
    }
  }

  /**
   * Implement the A (owner) side of the ECDH key exchange, per SDO protocol spec 1.12m 2.5.5.3.
   */
  private abstract static class Owner extends EcdhKeyExchange {

    Owner(KeyExchangeType type, String curve, int kexRandomSize, SecureRandom secureRandom) {
      super(type, curve, kexRandomSize, secureRandom);
    }

    @Override
    protected ByteBuffer generateSharedSecret(byte[] shx, byte[] myRnd, byte[] theirRnd) {

      ByteBuffer shSe = ByteBuffer.allocate(shx.length + myRnd.length + theirRnd.length);

      shSe.put(shx);
      shSe.put(theirRnd);
      shSe.put(myRnd);
      shSe.flip();

      return shSe;
    }
  }

  static class P256 {

    private static String CURVE = "P-256";
    private static int RANDOM_BYTES = 128 / 8;
    private static KeyExchangeType TYPE = KeyExchangeType.ECDH;

    static class Device extends EcdhKeyExchange.Device {

      public Device(SecureRandom secureRandom) {
        super(TYPE, CURVE, RANDOM_BYTES, secureRandom);
      }
    }

    static class Owner extends EcdhKeyExchange.Owner {

      public Owner(SecureRandom secureRandom) {
        super(TYPE, CURVE, RANDOM_BYTES, secureRandom);
      }
    }
  }

  static class P384 {

    private static String CURVE = "P-384";
    private static int RANDOM_BYTES = 384 / 8;
    private static KeyExchangeType TYPE = KeyExchangeType.ECDH384;

    static class Device extends EcdhKeyExchange.Device {

      public Device(SecureRandom secureRandom) {
        super(TYPE, CURVE, RANDOM_BYTES, secureRandom);
      }
    }

    static class Owner extends EcdhKeyExchange.Owner {

      public Owner(SecureRandom secureRandom) {
        super(TYPE, CURVE, RANDOM_BYTES, secureRandom);
      }
    }
  }

  private class State implements Serializable {

    private KeyPair myKeyPair;
    private byte[] myRandom;

    State(KeyPair keyPair, byte[] random) {
      this.setMyKeyPair(keyPair);
      this.setMyRandom(random);
    }

    KeyPair getMyKeyPair() {
      return myKeyPair;
    }

    void setMyKeyPair(KeyPair myKeyPair) {
      this.myKeyPair = myKeyPair;
    }

    byte[] getMyRandom() {
      return myRandom;
    }

    void setMyRandom(byte[] random) {
      this.myRandom = Arrays.copyOf(random, random.length);
    }
  }
}
