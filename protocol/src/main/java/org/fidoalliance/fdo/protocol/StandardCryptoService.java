// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.crypto.EntropySourceProvider;
import org.bouncycastle.crypto.fips.FipsDRBG;
import org.bouncycastle.crypto.util.BasicEntropySourceProvider;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.AsymKex;
import org.fidoalliance.fdo.protocol.message.CertChain;
import org.fidoalliance.fdo.protocol.message.CipherSuiteType;
import org.fidoalliance.fdo.protocol.message.CoseKey;
import org.fidoalliance.fdo.protocol.message.CoseKeyCurveType;
import org.fidoalliance.fdo.protocol.message.CoseProtectedHeader;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.CoseUnprotectedHeader;
import org.fidoalliance.fdo.protocol.message.CryptoKey;
import org.fidoalliance.fdo.protocol.message.DiffieHellman;
import org.fidoalliance.fdo.protocol.message.EcdhKex;
import org.fidoalliance.fdo.protocol.message.EncStructure;
import org.fidoalliance.fdo.protocol.message.Encrypt0;
import org.fidoalliance.fdo.protocol.message.EncryptionState;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KexMessage;
import org.fidoalliance.fdo.protocol.message.KexParty;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.Mac0;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.fidoalliance.fdo.protocol.message.SigInfo;
import org.fidoalliance.fdo.protocol.message.SigInfoType;
import org.fidoalliance.fdo.protocol.message.SigStructure;

public class StandardCryptoService implements CryptoService {

  public static final String X509_ALG_NAME = "X.509";
  public static final String VALIDATOR_ALG_NAME = "PKIX";

  private static final Provider BCFIPS = getInitializedProvider();
  protected static final SecureRandom random = getInitializedRandom();


  private static SecureRandom getInitializedRandom() {

    // DRBG -- Discrete Random Bit Generator.
    EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
    FipsDRBG.Builder drgbBldr = FipsDRBG.SHA512_HMAC.fromEntropySource(entSource)
            .setSecurityStrength(256)
            .setEntropyBitsRequired(256);
    return drgbBldr.build("nonce".getBytes(StandardCharsets.UTF_8), false);

  }

  private static Provider getInitializedProvider() {
    Security.addProvider(new PemProvider());
    Provider result = new BouncyCastleFipsProvider();
    Security.addProvider(result);
    return result;
  }

  /**
   * Gets secure Random generator.
   *
   * @return An instance of a secure random generator.
   */
  @Override
  public SecureRandom getSecureRandom() {
    return random;
  }


  /**
   * Gets Secure Random bytes form the internal Random number generator.
   *
   * @param size The number of bytes to get.
   * @return Byte array containing the random bytes.
   */
  @Override
  public byte[] getRandomBytes(int size) {
    final byte[] buffer = new byte[size];
    getSecureRandom().nextBytes(buffer);
    return buffer;
  }

  @Override
  public Provider getProvider() {
    return BCFIPS;
  }

  @Override
  public byte[] createHmacKey(HashType hashType) throws IOException {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA512", getProvider());
      switch (hashType) {
        case HMAC_SHA256:
          keyGenerator.init(256);
          break;
        case HMAC_SHA384:
          keyGenerator.init(384);
          break;
        default:
          throw new IOException(new IllegalArgumentException("not a hmac type"));
      }
      return keyGenerator.generateKey().toString().getBytes(StandardCharsets.UTF_8);
    } catch (RuntimeException e) {
      throw new RuntimeException(new IllegalArgumentException("not a hmac type"));
    } catch (Exception e) {
      throw new IOException(new IllegalArgumentException("not a hmac type"));
    }
  }

  @Override
  public KeyPair createKeyPair(PublicKeyType keyType, KeySizeType keySize) throws IOException {

    switch (keyType) {

      case RSA2048RESTR:
      case RSAPKCS:

        try {
          KeyPairGenerator kg = KeyPairGenerator.getInstance(
              new AlgorithmFinder().getAlgorithm(keyType), getProvider());

          RSAKeyGenParameterSpec rsaSpec =
              new RSAKeyGenParameterSpec(keySize.toInteger(), RSAKeyGenParameterSpec.F4);

          kg.initialize(rsaSpec, getSecureRandom());

          return kg.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
          throw new IOException(e);
        }

      case SECP384R1:
      case SECP256R1:

        try {

          final AlgorithmFinder algorithmFinder = new AlgorithmFinder();
          final CoseKeyCurveType coseKeyCurveType = algorithmFinder.getCoseKeyCurve(keyType);
          final String curveName = algorithmFinder.getAlgorithm(coseKeyCurveType);
          final KeyPairGenerator kg = KeyPairGenerator.getInstance(
              algorithmFinder.getAlgorithm(keyType), getProvider());
          ECGenParameterSpec ecSpec = new ECGenParameterSpec(curveName);
          kg.initialize(ecSpec, getSecureRandom());
          return kg.generateKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
          throw new IOException(e);
        }

      default:
        throw new IOException(new NoSuchAlgorithmException());
    }

  }


  @Override
  public Hash hash(HashType hashType, byte[] data) throws IOException {
    try {
      final String algName = new AlgorithmFinder().getAlgorithm(hashType);
      final MessageDigest digest = MessageDigest.getInstance(algName, getProvider());

      final Hash hash = new Hash();
      hash.setHashType(hashType);
      hash.setHashValue(digest.digest(data));

      return hash;

    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }


  @Override
  public Hash hash(HashType hashType, byte[] secret, byte[] data) throws IOException {
    SecretKey secretKey = null;
    try {

      Hash hash = new Hash();
      hash.setHashType(hashType);

      String algName = new AlgorithmFinder().getAlgorithm(hashType);
      final Mac mac = Mac.getInstance(algName, getProvider());

      final byte[] macData;
      secretKey = new SecretKeySpec(secret, algName);
      try {
        mac.init(secretKey);
        hash.setHashValue(mac.doFinal(data));
      } finally {
        destroyKey(secretKey);
      }

      return hash;

    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new IOException(e);
    } finally {
      destroyKey(secretKey);
    }
  }

  @Override
  public OwnerPublicKey encodeKey(PublicKeyType keyType, PublicKeyEncoding encType,
      Certificate[] chain) {

    OwnerPublicKey ownerKey = new OwnerPublicKey();
    ownerKey.setType(keyType);
    ownerKey.setEnc(encType);

    switch (encType) {
      case X509: {
        PublicKey publicKey = chain[0].getPublicKey();
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
        ownerKey.setBody(AnyType.fromObject(spec.getEncoded()));
      }
      break;
      case COSEX5CHAIN: {
        List<Certificate> certList = new ArrayList<>(Arrays.asList(chain));
        ownerKey.setBody(AnyType.fromObject(CertChain.fromList(certList)));
      }
      break;
      case COSEKEY: {
        final ECPublicKey ec = (ECPublicKey) chain[0].getPublicKey();
        final byte[] x = ec.getW().getAffineX().toByteArray();
        final byte[] y = ec.getW().getAffineY().toByteArray();

        final CoseKey coseKey = new CoseKey();
        coseKey.setX(x);
        coseKey.setY(y);
        coseKey.setCurve(new AlgorithmFinder().getCoseKeyCurve(keyType));
        ownerKey.setBody(AnyType.fromObject(coseKey));

      }
      break;
      case CRYPTO: {

        final RSAPublicKey key = (RSAPublicKey) chain[0].getPublicKey();
        final byte[] mod = key.getModulus().toByteArray();
        final byte[] exp = key.getPublicExponent().toByteArray();
        final CryptoKey cryptoKey = new CryptoKey();
        cryptoKey.setModulus(mod);
        cryptoKey.setExponent(exp);
        ownerKey.setBody(AnyType.fromObject(cryptoKey));

      }
      break;
      default:
        throw new NoSuchElementException();
    }

    return ownerKey;
  }

  @Override
  public PublicKey decodeKey(OwnerPublicKey ownerPublicKey) throws IOException {

    try {
      switch (ownerPublicKey.getEnc()) {
        case CRYPTO: {
          final CryptoKey key = ownerPublicKey.getBody().covertValue(CryptoKey.class);

          final BigInteger mod = new BigInteger(1, key.getModulus());
          final BigInteger exp = new BigInteger(1, key.getExponent());

          final RSAPublicKeySpec rsaPkSpec = new RSAPublicKeySpec(mod, exp);
          final KeyFactory factory = KeyFactory.getInstance(
              new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()), getProvider());
          return factory.generatePublic(rsaPkSpec);
        }
        case X509: {
          final byte[] x509body = ownerPublicKey.getBody().covertValue(byte[].class);
          final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509body);
          final KeyFactory factory = KeyFactory.getInstance(
              new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()), getProvider());

          return factory.generatePublic(keySpec);
        }
        case COSEX5CHAIN: {
          final CertChain chain = ownerPublicKey.getBody().covertValue(CertChain.class);
          return chain.getChain().get(0).getPublicKey();
        }
        case COSEKEY: {

          AlgorithmFinder algFinder = new AlgorithmFinder();
          AlgorithmParameters params = AlgorithmParameters.getInstance(
              algFinder.getAlgorithm(ownerPublicKey.getType()), getProvider());

          CoseKey coseKey = ownerPublicKey.getBody().covertValue(CoseKey.class);

          params.init(new ECGenParameterSpec(algFinder.getAlgorithm(coseKey.getCrv())));

          ECParameterSpec ecParameterSpec = params.getParameterSpec(ECParameterSpec.class);

          ECPoint ecPoint = new ECPoint(new BigInteger(1, coseKey.getX()),
              new BigInteger(1, coseKey.getY()));

          final KeyFactory factory = KeyFactory.getInstance(
              new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()), getProvider());

          return factory.generatePublic(
              new ECPublicKeySpec(ecPoint, ecParameterSpec));
        }
        default:
          throw new IOException(new IllegalArgumentException("key not valid"));
      }
    } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
      throw new IOException(e);
    }
  }

  @Override
  public CoseSign1 sign(byte[] payload, PrivateKey signingKey, OwnerPublicKey ownerKey)
      throws IOException {

    PublicKey publicKey = decodeKey(ownerKey);

    AlgorithmFinder finder = new AlgorithmFinder();
    CoseProtectedHeader cph = new CoseProtectedHeader();
    cph.setAlgId(finder.getCoseAlgorithm(ownerKey.getType(),
        finder.getKeySizeType(publicKey)));

    byte[] cphData = Mapper.INSTANCE.writeValue(cph);

    SigStructure sigStructure = new SigStructure();
    sigStructure.setContext("Signature1");
    sigStructure.setProtectedBody(cphData);
    sigStructure.setExternalData(new byte[0]);
    sigStructure.setPayload(payload);

    byte[] sigData = Mapper.INSTANCE.writeValue(sigStructure);

    try {

      String algName = finder.getSignatureAlgorithm(ownerKey.getType(),
          finder.getKeySizeType(publicKey));
      //todo: check for hsm provider
      Signature sig = Signature.getInstance(algName, getProvider());
      sig.initSign(signingKey);
      sig.update(sigData);
      byte[] derSign = sig.sign();
      byte[] finalSign = derSign;
      if (publicKey instanceof ECKey) {
        // COSE ECDSA signatures are not DER, but are instead R|S, with R and S padded to
        // key length and concatenated.  We must convert.
        BigInteger r;
        BigInteger s;
        try (ByteArrayInputStream bin = new ByteArrayInputStream(derSign);
            ASN1InputStream in = new ASN1InputStream(bin)) {

          DLSequence sequence = (DLSequence) in.readObject();
          r = ((ASN1Integer) sequence.getObjectAt(0)).getPositiveValue();
          s = ((ASN1Integer) sequence.getObjectAt(1)).getPositiveValue();
        }

        // PKCS11 keys cannot be directly interrogated, guess key size from associated algorithm IDs
        final int size;
        switch (ownerKey.getType()) {
          case SECP256R1:
            size = 32;
            break;
          case SECP384R1:
            size = 48;
            break;
          default:
            throw new InvalidParameterException("coseSignatureAlg " + ownerKey.getType());
        }
        finalSign = new byte[2 * size];
        BufferUtils.writeBigInteger(r, finalSign, 0, size);
        BufferUtils.writeBigInteger(s, finalSign, size, size);
      }

      CoseSign1 sign1 = new CoseSign1();
      sign1.setProtectedHeader(cphData);
      sign1.setUnprotectedHeader(new CoseUnprotectedHeader());
      sign1.setPayload(payload);
      sign1.setSignature(finalSign);

      return sign1;

    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      throw new IOException(e);
    }
  }

  protected KexMessage getAsymkexMessage(int randomSize,
      KexParty party,
      OwnerPublicKey ownerKey) throws IOException {

    KexMessage kexMessage = new KexMessage();
    AsymKex asymExchange = new AsymKex();

    switch (party) {

      case A:

        byte[] a = new byte[randomSize];
        getSecureRandom().nextBytes(a);

        kexMessage.setMessage(a);

        asymExchange.setRandomSize(randomSize);
        asymExchange.setB(a);
        asymExchange.setParty(party);
        kexMessage.setState(AnyType.fromObject(asymExchange));
        return kexMessage;

      case B:

        byte[] b = new byte[randomSize];
        getSecureRandom().nextBytes(b);

        byte[] xb;
        try {
          Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding",
              getProvider());
          cipher.init(Cipher.ENCRYPT_MODE, decodeKey(ownerKey), getSecureRandom());
          xb = cipher.doFinal(b);
        } catch (GeneralSecurityException e) {
          throw new IOException(e);
        }

        kexMessage.setMessage(xb);

        asymExchange.setRandomSize(randomSize);
        asymExchange.setB(b);
        asymExchange.setParty(party);
        kexMessage.setState(AnyType.fromObject(asymExchange));
        return kexMessage;

      default:
        throw new IllegalArgumentException(party.toString());
    }
  }

  protected KexMessage getEcdhMessage(PublicKeyType keyType, KeySizeType sizeType,
      KexParty party) throws IOException {
    final KeyPair kp = createKeyPair(keyType, sizeType);
    final ECPublicKey publicKey = (ECPublicKey) kp.getPublic();
    final int bitLength = publicKey.getParams().getCurve().getField().getFieldSize();
    final int byteLength = bitLength / Byte.SIZE;

    int ramdomLength;
    if (keyType.equals(PublicKeyType.SECP256R1)) {
      ramdomLength = 16;
    } else if (keyType.equals(PublicKeyType.SECP384R1)) {
      ramdomLength = 48;
    } else {
      throw new InvalidMessageException("");
    }
    final byte[] randomBytes = getRandomBytes(ramdomLength);

    //bstr[blen(x), x, blen(y), y, blen(Random), Random]
    final byte[] x = BufferUtils.adjustBigBuffer(publicKey.getW().getAffineX().toByteArray(),
        byteLength);
    final byte[] y = BufferUtils.adjustBigBuffer(publicKey.getW().getAffineY().toByteArray(),
        byteLength);

    try (ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      BufferUtils.writeLen(bao, x.length);
      bao.writeBytes(x);
      BufferUtils.writeLen(bao, y.length);
      bao.writeBytes(y);
      BufferUtils.writeLen(bao, randomBytes.length);
      bao.writeBytes(randomBytes);
      KexMessage msg = new KexMessage();
      msg.setMessage(bao.toByteArray());

      EcdhKex ecdhState = new EcdhKex();
      ecdhState.setKeyType(keyType);
      ecdhState.setEncodedKey(kp.getPrivate().getEncoded());
      ecdhState.setParty(party);

      msg.setState(AnyType.fromObject(ecdhState));
      return msg;
    }
  }

  @Override
  public KexMessage getKeyExchangeMessage(String kexSuiteName, KexParty party,
      OwnerPublicKey ownerKey) throws IOException {

    switch (kexSuiteName) {
      case "ECDH256":
        return getEcdhMessage(PublicKeyType.SECP256R1, KeySizeType.SIZE_256, party);
      case "ECDH384":
        return getEcdhMessage(PublicKeyType.SECP384R1, KeySizeType.SIZE_256, party);
      case "ASYMKEX2048":
        return getAsymkexMessage(32, party, ownerKey);
      case "ASYMKEX3072":
        return getAsymkexMessage(96, party, ownerKey);
      case DiffieHellman.DH14_ALG_NAME:
      case DiffieHellman.DH15_ALG_NAME:

        DiffieHellman.KeyExchange ke = DiffieHellman.buildKeyExchange(kexSuiteName, random);
        KexMessage msg = new KexMessage();
        try {
          msg.setMessage(ke.getMessage().toByteArray());
          msg.setState(AnyType.fromObject(ke));
          return msg;
        } finally {
          try {
            ke.destroy();
          } catch (DestroyFailedException e) {
            // this should never happen
            assert false;
            throw new RuntimeException(e);
          }
        }
      default:
        throw new InvalidMessageException("invalid key exchange " + kexSuiteName);
    }
  }

  protected List<byte[]> decodeEcdhMessage(byte[] message) throws IOException {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(message)) {

      //decodes a three part message
      List<byte[]> parts = new ArrayList<>();
      for (int i = 0; i < 3; i++) {

        //get the size of the three part message
        int len = (short) (((bis.read() & 0xFF) << 8) | (bis.read() & 0xFF));
        if (len < 0 || len >= message.length) {
          throw new IllegalArgumentException();
        }
        byte[] value = new byte[len];
        int read = bis.read(value);
        if (read > 0) {
          parts.add(value);
        }
      }
      return parts;

    }
  }

  protected KeyExchangeResult getEcdhSharedSecret(byte[] message, KexMessage ownState)
      throws IOException {
    try {
      //get ecdh 3 part message
      final List<byte[]> theirState = decodeEcdhMessage(message);
      final byte[] theirX = theirState.get(0);
      final byte[] theirY = theirState.get(1);
      final byte[] theirRandom = theirState.get(2);

      final EcdhKex ecdhState = ownState.getState().covertValue(EcdhKex.class);

      final byte[] encoded = ecdhState.getEncodedKey();

      final PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(encoded);
      final String algName = new AlgorithmFinder().getAlgorithm(ecdhState.getKeyType());
      final KeyFactory factory = KeyFactory.getInstance(algName, getProvider());
      final KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", getProvider());
      final ECPrivateKey ownKey = (ECPrivateKey) factory.generatePrivate(privateSpec);
      final byte[] sharedSecret;
      try {

        final ECPoint w = new ECPoint(new BigInteger(1, theirX), new BigInteger(1, theirY));
        final ECPublicKeySpec publicSpec = new ECPublicKeySpec(w, ownKey.getParams());

        final PublicKey theirKey = factory.generatePublic(publicSpec);

        keyAgreement.init(ownKey);
        keyAgreement.doPhase(theirKey, true);
        sharedSecret = keyAgreement.generateSecret();

      } finally {
        try {
          ownKey.destroy();
        } catch (DestroyFailedException e) {
          // many key implementations don't support destruction correctly - this exception
          // is expected and can be ignored.
        }
      }

      try {

        final List<byte[]> ownMessage = decodeEcdhMessage(ownState.getMessage());
        final byte[] ownRandom = ownMessage.get(2);
        final ByteBuffer buffer = ByteBuffer
            .allocate(sharedSecret.length + ownRandom.length + theirRandom.length);
        buffer.put(sharedSecret);

        if (ecdhState.getParty().equals(KexParty.A)) {
          //A is owner
          buffer.put(theirRandom);
          buffer.put(ownRandom);
        } else if (ecdhState.getParty().equals(KexParty.B)) {
          //B is device
          buffer.put(ownRandom);
          buffer.put(theirRandom);
        } else {
          throw new IllegalArgumentException();
        }

        buffer.flip();
        return new KeyExchangeResult(BufferUtils.unwrap(buffer), new byte[0]);

      } finally {
        Arrays.fill(sharedSecret, (byte) 0);
      }
    } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException e) {
      throw new IOException(e);
    }
  }

  protected KeyExchangeResult getAsymkexSharedSecret(
      byte[] message, KexMessage ownState, Key decryptionKey) throws IOException {

    AsymKex state = ownState.getState().covertValue(AsymKex.class);

    switch (state.getParty()) {
      case A:

        byte[] b;
        try {
          Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding",
                  getProvider());
          if (decryptionKey != null) {
            cipher.init(Cipher.DECRYPT_MODE, decryptionKey, getSecureRandom());
            b = cipher.doFinal(message);
          } else {
            throw new IllegalStateException("Decryption Key is empty");
          }
        } catch (GeneralSecurityException e) {
          throw new RuntimeException(e);
        }
        return new KeyExchangeResult(b, ownState.getMessage());

      case B:
        return new KeyExchangeResult(state.getB(), message);

      default:
        throw new IllegalArgumentException();
    }

  }

  @Override
  public KeyExchangeResult getSharedSecret(String suiteName, byte[] message, KexMessage ownState,
      Key decryptionKey) throws IOException {

    switch (suiteName) {
      case "ECDH256":
      case "ECDH384":
        return getEcdhSharedSecret(message, ownState);
      case "ASYMKEX2048":
      case "ASYMKEX3072":
        return getAsymkexSharedSecret(message, ownState, decryptionKey);
      case DiffieHellman.DH14_ALG_NAME:
      case DiffieHellman.DH15_ALG_NAME:

        DiffieHellman.KeyExchange ke =
            ownState.getState().covertValue(DiffieHellman.KeyExchange.class);

        try {
          return new KeyExchangeResult(
              ke.computeSharedSecret(new BigInteger(1, message)).toByteArray(), new byte[0]);
        } finally {
          try {
            ke.destroy();
          } catch (DestroyFailedException e) {
            // this should never happen
            assert false;
            throw new RuntimeException(e);
          }
        }
      default:
        throw new IOException(new NoSuchAlgorithmException(suiteName));
    }
  }

  @Override
  public EncryptionState getEncryptionState(KeyExchangeResult kxResult, CipherSuiteType cipherType)
      throws IOException {

    EncryptionState state = new EncryptionState();

    state.setCipherSuite(cipherType);

    // The second key in the state object must be a Composite of at most two values:
    // the SEK and the SVK, or
    // the SEVK, which is how we'll treat a single (SEK) key.
    final int sekSize;
    final int svkSize;
    final String prfId;

    switch (cipherType) {

      case COSE_AES128_CTR:
      case COSE_AES128_CBC:

        // 128-bit AES (SEK), 256-bit HMAC (SVK)
        sekSize = 16;
        svkSize = 32;
        prfId = "HmacSHA256";// Cipher Suite Names and Meanings in FDO spec
        break;

      case COSE_AES256_CTR:
      case COSE_AES256_CBC:

        // 256-bit AES (SEK), 512-bit HMAC (SVK) (HMAC-384 needs a 512-bit key)
        sekSize = 32;
        svkSize = 64;
        prfId = "HmacSHA384"; // Cipher Suite Names and Meanings in FDO spec

        break;
      case A128GCM:
      case AES_CCM_64_128_128:

        // 128-bit AES (SEVK)
        sekSize = 16;
        svkSize = 0;
        prfId = "HmacSHA256"; //Cipher Suite Names and Meanings in FDO spec

        break;
      case A256GCM:
      case AES_CCM_64_128_256:

        // 256-bit AES (SEVK)
        sekSize = 32;
        svkSize = 0;
        prfId = "HmacSHA256"; //Cipher Suite Names and Meanings in FDO spec

        break;
      default:
        throw new IllegalArgumentException("unrecognized cipher suite: " + cipherType);

    }

    final byte[] keyMaterial;
    try {
      keyMaterial = kdf((sekSize + svkSize) * Byte.SIZE, prfId, kxResult);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    state.setSek(Arrays.copyOfRange(keyMaterial, 0, sekSize));
    state.setSev(Arrays.copyOfRange(keyMaterial, sekSize, sekSize + svkSize));

    // CTR modes need specialized IV information
    if (isCtrCipher(cipherType)) {
      state.setIv(getCtrIv());
      state.setCounter(0L);
    }

    return state;
  }

  protected byte[] getCtrIv() {
    ByteBuffer buffer = ByteBuffer.allocate(16);
    byte[] seed = getRandomBytes(12);
    buffer.put(seed);
    buffer.put(new byte[]{0, 0, 0, 0});
    buffer.flip();
    return BufferUtils.unwrap(buffer);
  }

  protected boolean isCtrCipher(CipherSuiteType cipherType) {
    return CipherSuiteType.COSE_AES128_CTR.equals(cipherType)
        || CipherSuiteType.COSE_AES256_CTR.equals(cipherType);
  }

  protected boolean isGcmCipher(CipherSuiteType cipherType) {
    return CipherSuiteType.A128GCM.equals(cipherType)
        || CipherSuiteType.A256GCM.equals(cipherType);
  }

  protected boolean isCcmCipher(CipherSuiteType cipherType) {
    return CipherSuiteType.AES_CCM_16_128_128.equals(cipherType)
        || CipherSuiteType.AES_CCM_16_128_256.equals(cipherType)
        || CipherSuiteType.AES_CCM_64_128_128.equals(cipherType)
        || CipherSuiteType.AES_CCM_64_128_256.equals(cipherType);
  }

  protected boolean isCcbCipher(CipherSuiteType cipherType) {
    return CipherSuiteType.COSE_AES128_CBC.equals(cipherType)
        || CipherSuiteType.COSE_AES256_CBC.equals(cipherType);
  }

  // Key Derivation Function (KDF).
  //
  // See NIST SP 800-108, FDO spec section 3.6.4
  // Where possible, variable names are chosen to match those documents.
  protected byte[] kdf(
      int size,      // the number of bits to derive (L)
      String prfId,  // the JCE ID of the PRF to use
      KeyExchangeResult kxResult) // the sharedSecret and contextRandom
      throws
      InvalidKeyException,
      IOException,
      NoSuchAlgorithmException {

    Mac prf = Mac.getInstance(prfId, getProvider());
    prf.init(new SecretKeySpec(kxResult.shSe, prfId));

    final int h = prf.getMacLength() * Byte.SIZE; // (h) the length (in bits) of the PRF output
    final int l = size;  // (L) the length (in bits) of the derived keying material
    // (n) the number of iterations of the PRF needed to generate L bits of
    // keying material.
    final int n = Double.valueOf(Math.ceil((double) l / (double) h)).intValue();

    ByteArrayOutputStream result = new ByteArrayOutputStream();

    for (int i = 1; n >= i; i++) { // NIST SP 800-108 loops from 1 to n, not 0 to n - 1!

      prf.reset();

      // write K(i) to the prf...
      prf.update((byte) i); // [i]2
      prf.update("FIDO-KDF".getBytes(StandardCharsets.UTF_8)); // Label
      prf.update((byte) 0); // 0x00, separator
      prf.update("AutomaticOnboardTunnel".getBytes(StandardCharsets.UTF_8)); // Context (part 1)
      prf.update(kxResult.contextRand);                                      // Context (part 2)
      prf.update((byte) ((l >> 8) & 0xff)); // [L]2, upper byte
      prf.update((byte) (l & 0xff));        // [L]2, lower byte

      result.write(prf.doFinal());  // append K(i) to the cumulative result
    }

    return result.toByteArray();
  }


  @Override
  public byte[] encrypt(byte[] payload, EncryptionState state) throws IOException {
    try {
      final CipherSuiteType cipherType = state.getCipherSuite();

      final byte[] sek = state.getSek();
      final byte[] sev = state.getSev();
      final Key keySpec = new SecretKeySpec(sek, "AES");

      final byte[] iv;
      if (isCtrCipher(cipherType)) {
        iv = state.getIv();
      } else if (isGcmCipher(cipherType)) { // GCM uses a 12-byte IV
        iv = getRandomBytes(12);
      } else if (isCcmCipher(cipherType)) { // CCM modes use a 7-byte nonce
        iv = getRandomBytes(7);
      } else { // all other ciphers use a random IV, AES only uses 16 bytes despite key length
        iv = getRandomBytes(16);
      }

      CoseProtectedHeader cph = new CoseProtectedHeader();
      cph.setAlgId(cipherType.toInteger());

      byte[] cphData = Mapper.INSTANCE.writeValue(cph);

      byte[] aad;
      if (isCcmCipher(cipherType) || isGcmCipher(cipherType)) {
        // Simple encrypt0 types use AAD as described in the COSE spec
        EncStructure encStructure = new EncStructure();
        encStructure.setContext("Encrypt0");
        encStructure.setProtectedHeader(cphData);
        encStructure.setExternal(new byte[0]);
        aad = Mapper.INSTANCE.writeValue(encStructure);


      } else {
        aad = new byte[0];
      }

      byte[] ciphered = null;
      if (isCcmCipher(cipherType)) {

        try {
          ciphered = ccmEncrypt(Cipher.ENCRYPT_MODE, payload, keySpec, iv, aad);
        } catch (Exception e) {
          throw new IOException(e);
        }

      } else {

        AlgorithmParameterSpec cipherParams;
        if (isGcmCipher(cipherType)) { // GCM ciphers use GCMParameterSpec

          // According to NIST SP800.38D section 5.2.1.2, the tag length can
          // only be 96, 104, 112, 120, or 128 bits.
          if (!Arrays.asList(96, 104, 112, 120, 128).contains(128)) {
            throw new IllegalArgumentException("illegal GCM tag length");
          }
          cipherParams = new GCMParameterSpec(128, iv);

        } else {
          cipherParams = new IvParameterSpec(iv);
        }

        final Cipher cipher = aesTypeToCipher(cipherType);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, cipherParams);

        // Since AAD can be no more than 2^64 - 1 bits and a Java array can be
        // no longer than 2^31 - 1 elements, there's no need to length check
        // the AAD.
        cipher.updateAAD(aad, 0, aad.length);

        // Since GCM plaintext can be no more than 2^39 - 256 bits and a Java
        // array can be no longer than 2^31 - 1 elements, there's no need
        // to length check the payload.
        ciphered = cipher.doFinal(payload);

      }

      byte[] message = encryptThenMac(sev, ciphered, iv, cipherType);

      if (isCtrCipher(cipherType)) {
        updateIv(ciphered.length, state);
      }

      return message;

    } catch (InvalidAlgorithmParameterException e) {
      throw new IOException(e);
    } catch (IllegalBlockSizeException e) {
      throw new IOException(e);
    } catch (BadPaddingException e) {
      throw new IOException(e);
    } catch (InvalidKeyException e) {
      throw new IOException(e);
    }


  }


  @Override
  public byte[] decrypt(byte[] message, EncryptionState state) throws IOException {
    try {

      final byte[] sek = state.getSek();
      final byte[] svk = state.getSev();
      final Key keySpec = new SecretKeySpec(sek, "AES");
      final CipherSuiteType cipherType = state.getCipherSuite();

      Encrypt0 encrypt0 = null;
      final byte[] aad;

      if (isSimpleEncryptedMessage(cipherType)) {

        encrypt0 = Mapper.INSTANCE.readValue(message, Encrypt0.class);
        EncStructure encStructure = new EncStructure();
        encStructure.setContext("Encrypt0");
        encStructure.setProtectedHeader(encrypt0.getProtectedHeader());
        encStructure.setExternal(new byte[0]);
        aad = Mapper.INSTANCE.writeValue(encStructure);

      } else { // legacy (composed) message

        Mac0 mac0 = Mapper.INSTANCE.readValue(message, Mac0.class);
        encrypt0 = Mapper.INSTANCE.readValue(mac0.getPayload(), Encrypt0.class);

        aad = new byte[0];
        CoseProtectedHeader cph = Mapper.INSTANCE.readValue(encrypt0.getProtectedHeader(),
            CoseProtectedHeader.class);
        HashType macType = HashType.fromNumber(cph.getAlgId());

        final byte[] mac1 = mac0.getTagValue();

        Hash mac2 = hash(macType, svk, mac0.getPayload());
        if (!Arrays.equals(mac2.getHashValue(), mac1)) {
          throw new InvalidMessageException("Mac0 hashes does not match");
        }

      }

      final CoseUnprotectedHeader uph = encrypt0.getUnprotectedHeader();
      final byte[] iv = uph.getIv();

      if (isCcmCipher(cipherType)) {

        try {
          final byte[] ciphered = encrypt0.getCipherText();
          return ccmEncrypt(Cipher.DECRYPT_MODE, ciphered, keySpec, iv, aad);
        } catch (Exception e) {
          throw new IOException(e);
        }

      } else {

        AlgorithmParameterSpec cipherParams;
        if (isGcmCipher(cipherType)) { // GCM ciphers use GCMParameterSpec
          cipherParams = new GCMParameterSpec(128, iv);
        } else {
          cipherParams = new IvParameterSpec(iv);
        }

        final Cipher cipher = aesTypeToCipher(cipherType);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, cipherParams);
        cipher.updateAAD(aad, 0, aad.length);

        final byte[] ciphered = encrypt0.getCipherText();
        return cipher.doFinal(ciphered);

      }
    } catch (InvalidAlgorithmParameterException | InvalidKeyException
        | BadPaddingException | IllegalBlockSizeException e) {
      throw new IOException(e);
    }
  }

  protected void updateIv(int cipheredLen, EncryptionState state) throws IOException {

    final ByteBuffer iv = ByteBuffer.wrap(state.getIv());
    long counter = state.getCounter();
    if (isCtrCipher(state.getCipherSuite())) {
      //the last 4 bytes of the iv will be the counter
      byte[] seed = new byte[12];
      iv.get(seed);

      int blockCount = 1 + (cipheredLen - 1) / 16;
      counter += blockCount;

      ByteBuffer buffer = ByteBuffer.allocate(16);
      buffer.put(seed);

      buffer.putInt((int) counter);
      buffer.flip();

      state.setIv(BufferUtils.unwrap(buffer));
      state.setCounter(counter);

    } else if (isCcbCipher(state.getCipherSuite())) {
      byte[] seed = new byte[12];
      iv.get(seed);

      byte[] rnd = getRandomBytes(4);

      ByteBuffer buffer = ByteBuffer.allocate(16);
      buffer.put(seed);
      buffer.put(rnd);
      buffer.flip();
      state.setIv(BufferUtils.unwrap(buffer));

    } else {
      throw new IOException(new NoSuchAlgorithmException());
    }
  }

  protected byte[] ccmEncrypt(int encryptMode, byte[] payload, Key keySpec, byte[] iv,
                              byte[] aad) throws InvalidAlgorithmParameterException,
          InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException,
          IllegalBlockSizeException, BadPaddingException {
    AlgorithmParameterSpec cipherParams;
    cipherParams = new GCMParameterSpec(128, iv);
    final Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding", getProvider());
    cipher.init(encryptMode, keySpec, cipherParams);
    cipher.updateAAD(aad, 0, aad.length);
    return cipher.doFinal(payload);
  }

  protected byte[] encryptThenMac(byte[] secret, byte[] ciphered, byte[] iv,
      CipherSuiteType cipherType) throws IOException {

    CoseProtectedHeader cph = new CoseProtectedHeader();
    cph.setAlgId(cipherType.toInteger());

    CoseUnprotectedHeader uph = new CoseUnprotectedHeader();
    uph.setIv(iv);

    Encrypt0 encrypt0 = new Encrypt0();
    encrypt0.setProtectedHeader(Mapper.INSTANCE.writeValue(cph));
    encrypt0.setUnprotectedHeader(uph);
    encrypt0.setCipherText(ciphered);

    if (isSimpleEncryptedMessage(
        cipherType)) { // not all encrypted messages use the 'composed' type
      return Mapper.INSTANCE.writeValue(encrypt0);
    }

    HashType hmacType;
    if (cipherType.equals(CipherSuiteType.COSE_AES128_CTR)
        || cipherType.equals(CipherSuiteType.COSE_AES128_CBC)) {
      hmacType = HashType.HMAC_SHA256;
    } else if (cipherType.equals(CipherSuiteType.COSE_AES256_CBC)
            || cipherType.equals(CipherSuiteType.COSE_AES256_CTR)) {
      hmacType = HashType.SHA384;
    } else {
      throw new IOException(new NoSuchAlgorithmException());
    }

    cph = new CoseProtectedHeader();
    cph.setAlgId(hmacType.toInteger());

    Mac0 mac0 = new Mac0();
    mac0.setProtectedHeader(Mapper.INSTANCE.writeValue(cph));
    mac0.setUnprotectedHeader(new CoseUnprotectedHeader());
    byte[] payload = Mapper.INSTANCE.writeValue(encrypt0);
    mac0.setPayload(payload);

    Hash mac = hash(hmacType, secret, payload);
    mac0.setTagValue(mac.getHashValue());

    return Mapper.INSTANCE.writeValue(mac0);
  }

  protected Cipher aesTypeToCipher(CipherSuiteType cipherType) throws IOException {

    try {

      switch (cipherType) {
        case COSE_AES128_CTR:
        case COSE_AES256_CTR:
          return Cipher.getInstance("AES/CTR/NoPadding", getProvider());

        case COSE_AES128_CBC:
        case COSE_AES256_CBC:
          return Cipher.getInstance("AES/CBC/PKCS7Padding", getProvider());

        case A128GCM:
        case A256GCM:
          return Cipher.getInstance("AES/GCM/NoPadding", getProvider());

        default:
          throw new UnsupportedOperationException("AESType: " + cipherType);
      }
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IOException(e);
    }
  }

  private boolean isSimpleEncryptedMessage(CipherSuiteType aesType) {
    return CipherSuiteType.A128GCM == aesType
        || CipherSuiteType.A256GCM == aesType
        || CipherSuiteType.AES_CCM_16_128_128 == aesType
        || CipherSuiteType.AES_CCM_16_128_256 == aesType
        || CipherSuiteType.AES_CCM_64_128_128 == aesType
        || CipherSuiteType.AES_CCM_64_128_256 == aesType;
  }

  @Override
  public boolean verify(CoseSign1 message, SigInfo sigInfo)
      throws IOException {
    SigStructure sigStructure = new SigStructure();
    sigStructure.setContext("Signature1");
    sigStructure.setProtectedBody(message.getProtectedHeader());
    sigStructure.setExternalData(new byte[0]);
    sigStructure.setPayload(message.getPayload());

    byte[] signature = message.getSignature();
    byte[] sigData = Mapper.INSTANCE.writeValue(sigStructure);
    byte[] maroePrefix = message.getUnprotectedHeader().getMaroPrefix();

    SigInfoType sigInfoType = sigInfo.getSigInfoType();
    byte[] groupId = sigInfo.getInfo();

    EpidService epidService = new EpidService();
    return epidService.verifyEpidSignature(
        signature,
        maroePrefix,
        message.getUnprotectedHeader().getEatNonce().getNonce(),
        sigData,
        groupId,
        sigInfoType);
  }

  @Override
  public boolean verify(CoseSign1 message, OwnerPublicKey ownerKey) throws IOException {

    SigStructure sigStructure = new SigStructure();
    sigStructure.setContext("Signature1");
    sigStructure.setProtectedBody(message.getProtectedHeader());
    sigStructure.setExternalData(new byte[0]);
    sigStructure.setPayload(message.getPayload());

    byte[] sigData = Mapper.INSTANCE.writeValue(sigStructure);
    byte[] derSig = message.getSignature();
    byte[] maroePrefix = message.getUnprotectedHeader().getMaroPrefix();

    PublicKey publicKey = decodeKey(ownerKey);
    if (publicKey instanceof ECKey) {
      // The encoded signature is fixed-width r|s concatenated, we must convert it to DER.
      int size = message.getSignature().length / 2;
      ASN1Integer r =
          new ASN1Integer(new BigInteger(1, message.getSignature(), 0, size));
      ASN1Integer s =
          new ASN1Integer(new BigInteger(1, message.getSignature(), size, size));
      DLSequence sequence = new DLSequence(new ASN1Encodable[]{r, s});
      ByteArrayOutputStream sigBytes = new ByteArrayOutputStream();
      ASN1OutputStream asn1out = new ASN1OutputStream(sigBytes);
      asn1out.writeObject(sequence);
      byte[] b = sigBytes.toByteArray();
      derSig = Arrays.copyOf(b, b.length);

      // if MAROE based signature - prefix signature data with maroe prefix
      if (maroePrefix != null && maroePrefix.length > 0) {
        try {
          ByteArrayOutputStream bas = new ByteArrayOutputStream();
          bas.write(maroePrefix);
          bas.write(sigData);
          sigData = bas.toByteArray();
        } catch (IOException ex) {
          // should never get here
          return false;
        }
      }
    }

    try {
      AlgorithmFinder finder = new AlgorithmFinder();

      String algName = finder.getSignatureAlgorithm(ownerKey.getType(),
          finder.getKeySizeType(publicKey));

      Signature sig = Signature.getInstance(algName, getProvider());
      sig.initVerify(publicKey);
      sig.update(sigData);
      return sig.verify(derSig);

    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      throw new IOException(e);
    }
  }

  @Override
  public SigInfo getSigInfoB(SigInfo sigInfoA) throws IOException {
    if (null != sigInfoA && sigInfoA.getInfo().length > 0
        && (sigInfoA.getSigInfoType().equals(SigInfoType.EPID10)
        || sigInfoA.getSigInfoType().equals(SigInfoType.EPID11))) {

      EpidService epidMaterialService = new EpidService();
      try {
        return epidMaterialService.getSigInfo(sigInfoA);
      } catch (IOException ioException) {
        throw new InvalidMessageException(new IllegalArgumentException());
      }
    }
    return sigInfoA;
  }

  @Override
  public void destroyKey(KeyPair pair) {
    if (pair != null) {
      destroyKey(pair.getPrivate());
    }
  }

  @Override
  public void destroyKey(PrivateKey privateKey) {
    if (privateKey != null && !privateKey.isDestroyed()) {
      try {
        privateKey.destroy();
      } catch (DestroyFailedException e) {
        //crypto lib does not support destroy
      }
    }
  }

  @Override
  public void destroyKey(SecretKey key) {
    if (key != null && !key.isDestroyed()) {
      try {
        key.destroy();
      } catch (DestroyFailedException e) {
        //crypto lib does not support destroy
      }
    }
  }


}
