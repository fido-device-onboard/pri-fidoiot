//package org.fidoalliance.fdo.protocol;
//
//import org.bouncycastle.asn1.*;
//import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
//import org.bouncycastle.util.Strings;
//import org.bouncycastle.util.encoders.Hex;
//import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
//import org.fidoalliance.fdo.protocol.message.*;
//
//import javax.crypto.KeyGenerator;
//import javax.crypto.Mac;
//import javax.crypto.SecretKey;
//import javax.crypto.spec.SecretKeySpec;
//import javax.security.auth.DestroyFailedException;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.math.BigInteger;
//import java.nio.charset.StandardCharsets;
//import java.security.*;
//import java.security.cert.Certificate;
//import java.security.interfaces.ECKey;
//import java.security.interfaces.ECPublicKey;
//import java.security.interfaces.RSAPublicKey;
//import java.security.spec.*;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.NoSuchElementException;
//
//class ExValues
//{
//    public static final long THIRTY_DAYS = 1000L * 60 * 60 * 24 * 30;
//    public static final SecretKey SampleAesKey =
//            new SecretKeySpec(Hex.decode("000102030405060708090a0b0c0d0e0f"), "AES");
//    public static final SecretKey SampleTripleDesKey =
//            new SecretKeySpec(Hex.decode("000102030405060708090a0b0c0d0e0f1011121314151617"),
//            "TripleDES");
//    public static final SecretKey SampleHMacKey =
//            new SecretKeySpec(Hex.decode("000102030405060708090a0b0c0d0e0f10111213"),
//            "HmacSHA512");
//    public static final byte[] SampleInput = Strings.toByteArray("Hello World!");
//    public static final byte[] SampleTwoBlockInput
//            = Strings.toByteArray("Some cipher modes require more than one block");
//    public static final byte[] Nonce = Strings.toByteArray("number only used once");
//    public static final byte[] PersonalizationString
//            = Strings.toByteArray("a constant personal marker");
//    public static final byte[] Initiator = Strings.toByteArray("Initiator");
//    public static final byte[] Recipient = Strings.toByteArray("Recipient");
//    public static final byte[] UKM = Strings.toByteArray("User keying material");
//}
//
//
//public class CustomCryptoService implements CryptoService {
//
//    public static final String X509_ALG_NAME = "X.509";
//    public static final String VALIDATOR_ALG_NAME = "PKIX";
//
//
//    protected static final SecureRandom random = getInitializedRandom();
//    private static final Provider BCFIPS = getInitializedProvider();
//
//    private static SecureRandom getInitializedRandom() {
//
//        try {
//            SecureRandom random = SecureRandom.getInstance("DEFAULT", BCFIPS);
//            return random;
//        } catch (Exception e) {
//            throw new RuntimeException("Unable to initialize secure random.", e);
//        }
//    }
//
//    private static Provider getInitializedProvider() {
//        Provider result = new BouncyCastleFipsProvider();
//        Security.addProvider(result);
//        return result;
//    }
//
//
//
//    @Override
//    public SecureRandom getSecureRandom() {
//        return random;
//    }
//
//    @Override
//    public byte[] getRandomBytes(int size) {
//        final byte[] buffer = new byte[size];
//        getSecureRandom().nextBytes(buffer);
//        return buffer;
//    }
//
//    @Override
//    public Provider getProvider() {
//        return BCFIPS;
//    }
//
//    @Override
//    public byte[] createHmacKey(HashType hashType) throws IOException {
//        try {
//            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA512", "BCFIPS");
//            switch (hashType) {
//                case HMAC_SHA256:
//                    keyGenerator.init(256);
//                case HMAC_SHA384:
//                    keyGenerator.init(384);
//            }
//
//            return keyGenerator.generateKey().toString().getBytes(StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            throw new IOException(new IllegalArgumentException("not a hmac type"));
//        }
//    }
//
//    @Override
//    public KeyPair createKeyPair(PublicKeyType keyType,
//    KeySizeType keySize) throws IOException {
//
//        switch (keyType) {
//
//            case RSA2048RESTR:
//            case RSAPKCS:
//
//                try {
//                    KeyPairGenerator kg = KeyPairGenerator.getInstance(
//                            new AlgorithmFinder().getAlgorithm(keyType), "BCFIPS");
//
//                    RSAKeyGenParameterSpec rsaSpec =
//                            new RSAKeyGenParameterSpec(keySize.toInteger(),
//                            RSAKeyGenParameterSpec.F4);
//
//                    kg.initialize(rsaSpec, getSecureRandom());
//
//                    return kg.generateKeyPair();
//                } catch (NoSuchAlgorithmException |
//                InvalidAlgorithmParameterException | NoSuchProviderException e) {
//                    throw new IOException(e);
//                }
//
//            case SECP384R1:
//            case SECP256R1:
//
//                try {
//
//                    final AlgorithmFinder algorithmFinder = new AlgorithmFinder();
//                    final CoseKeyCurveType coseKeyCurveType =
//                    algorithmFinder.getCoseKeyCurve(keyType);
//                    final String curveName = algorithmFinder.getAlgorithm(coseKeyCurveType);
//                    final KeyPairGenerator kg = KeyPairGenerator.getInstance(
//                            algorithmFinder.getAlgorithm(keyType), "BCFIPS");
//                    ECGenParameterSpec ecSpec = new ECGenParameterSpec(curveName);
//                    kg.initialize(ecSpec, getSecureRandom());
//                    return kg.generateKeyPair();
//                } catch (InvalidAlgorithmParameterException
//                | NoSuchAlgorithmException | NoSuchProviderException e) {
//                    throw new IOException(e);
//                }
//
//            default:
//                throw new IOException(new NoSuchAlgorithmException());
//        }
//
//    }
//
//    @Override
//    public Hash hash(HashType hashType, byte[] data) throws IOException {
//        try {
//            final String algName = new AlgorithmFinder().getAlgorithm(hashType);
//            final MessageDigest digest = MessageDigest.getInstance(algName, getProvider());
//
//            final Hash hash = new Hash();
//            hash.setHashType(hashType);
//            hash.setHashValue(digest.digest(data));
//
//            return hash;
//
//        } catch (NoSuchAlgorithmException e) {
//            throw new IOException(e);
//        }
//    }
//
//    @Override
//    public Hash hash(HashType hashType, byte[] secret, byte[] data) throws IOException {
//        SecretKey secretKey = null;
//        try {
//
//            Hash hash = new Hash();
//            hash.setHashType(hashType);
//
//            String algName = new AlgorithmFinder().getAlgorithm(hashType);
//            final Mac mac = Mac.getInstance(algName, getProvider());
//
//            secretKey = new SecretKeySpec(secret, algName);
//            try {
//                mac.init(secretKey);
//                hash.setHashValue(mac.doFinal(data));
//            } finally {
//                destroyKey(secretKey);
//            }
//
//            return hash;
//
//        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
//            throw new IOException(e);
//        } finally {
//            destroyKey(secretKey);
//        }
//    }
//
//    @Override
//    public OwnerPublicKey encodeKey(PublicKeyType keyType,
//    PublicKeyEncoding encType, Certificate[] chain) {
//        OwnerPublicKey ownerKey = new OwnerPublicKey();
//        ownerKey.setType(keyType);
//        ownerKey.setEnc(encType);
//
//        switch (encType) {
//            case X509: {
//                PublicKey publicKey = chain[0].getPublicKey();
//                X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
//                ownerKey.setBody(AnyType.fromObject(spec.getEncoded()));
//            }
//            break;
//            case COSEX5CHAIN: {
//                List<Certificate> certList = new ArrayList<>(Arrays.asList(chain));
//                ownerKey.setBody(AnyType.fromObject(CertChain.fromList(certList)));
//            }
//            break;
//            case COSEKEY: {
//                final ECPublicKey ec = (ECPublicKey) chain[0].getPublicKey();
//                final byte[] x = ec.getW().getAffineX().toByteArray();
//                final byte[] y = ec.getW().getAffineY().toByteArray();
//
//                final CoseKey coseKey = new CoseKey();
//                coseKey.setX(x);
//                coseKey.setY(y);
//                coseKey.setCurve(new AlgorithmFinder().getCoseKeyCurve(keyType));
//                ownerKey.setBody(AnyType.fromObject(coseKey));
//
//            }
//            break;
//            case CRYPTO: {
//
//                final RSAPublicKey key = (RSAPublicKey) chain[0].getPublicKey();
//                final byte[] mod = key.getModulus().toByteArray();
//                final byte[] exp = key.getPublicExponent().toByteArray();
//                final CryptoKey cryptoKey = new CryptoKey();
//                cryptoKey.setModulus(mod);
//                cryptoKey.setExponent(exp);
//                ownerKey.setBody(AnyType.fromObject(cryptoKey));
//
//            }
//            break;
//            default:
//                throw new NoSuchElementException();
//        }
//
//        return ownerKey;
//    }
//
//    @Override
//    public PublicKey decodeKey(OwnerPublicKey ownerPublicKey) throws IOException {
//        try {
//            switch (ownerPublicKey.getEnc()) {
//                case CRYPTO: {
//                    final CryptoKey key = ownerPublicKey.getBody().covertValue(CryptoKey.class);
//
//                    final BigInteger mod = new BigInteger(1, key.getModulus());
//                    final BigInteger exp = new BigInteger(1, key.getExponent());
//
//                    final RSAPublicKeySpec rsaPkSpec = new RSAPublicKeySpec(mod, exp);
//                    final KeyFactory factory = KeyFactory.getInstance(
//                            new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()),
//                            BCFIPS);
//                    return factory.generatePublic(rsaPkSpec);
//                }
//                case X509: {
//                    final byte[] x509body = ownerPublicKey.getBody().covertValue(byte[].class);
//                    final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509body);
//                    final KeyFactory factory = KeyFactory.getInstance(
//                            new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()),
//                            BCFIPS);
//
//                    return factory.generatePublic(keySpec);
//                }
//                case COSEX5CHAIN: {
//                    final CertChain chain =
//                      ownerPublicKey.getBody().covertValue(CertChain.class);
//                    return chain.getChain().get(0).getPublicKey();
//                }
//                case COSEKEY: {
//
//                    AlgorithmFinder algFinder = new AlgorithmFinder();
//                    AlgorithmParameters params = AlgorithmParameters.getInstance(
//                            algFinder.getAlgorithm(ownerPublicKey.getType()), BCFIPS);
//
//                    CoseKey coseKey = ownerPublicKey.getBody().covertValue(CoseKey.class);
//
//                    params.init(
//                    new ECGenParameterSpec(algFinder.getAlgorithm(coseKey.getCrv())));
//
//                    ECParameterSpec ecParameterSpec =
//                    params.getParameterSpec(ECParameterSpec.class);
//
//                    ECPoint ecPoint = new ECPoint(new BigInteger(1, coseKey.getX()),
//                            new BigInteger(1, coseKey.getY()));
//
//                    final KeyFactory factory = KeyFactory.getInstance(
//                      new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()), BCFIPS);
//
//                    return factory.generatePublic(
//                            new ECPublicKeySpec(ecPoint, ecParameterSpec));
//                }
//                default:
//                    throw new IOException(new IllegalArgumentException("key not valid"));
//            }
//        } catch (InvalidKeySpecException |
//        NoSuchAlgorithmException | InvalidParameterSpecException e) {
//            throw new IOException(e);
//        }
//    }
//
//    @Override
//    public SigInfo getSigInfoB(SigInfo sigInfoA) throws IOException {
//        if (null != sigInfoA && sigInfoA.getInfo().length > 0
//                && (sigInfoA.getSigInfoType().equals(SigInfoType.EPID10)
//                || sigInfoA.getSigInfoType().equals(SigInfoType.EPID11))) {
//
//            EpidService epidMaterialService = new EpidService();
//            try {
//                return epidMaterialService.getSigInfo(sigInfoA);
//            } catch (IOException ioException) {
//                throw new InvalidMessageException(new IllegalArgumentException());
//            }
//        }
//        return sigInfoA;
//    }
//
//    @Override
//    public CoseSign1 sign(byte[] payload,
//    PrivateKey signingKey, OwnerPublicKey ownerKey) throws IOException {
//        PublicKey publicKey = decodeKey(ownerKey);
//
//        AlgorithmFinder finder = new AlgorithmFinder();
//        CoseProtectedHeader cph = new CoseProtectedHeader();
//        cph.setAlgId(finder.getCoseAlgorithm(ownerKey.getType(),
//                finder.getKeySizeType(publicKey)));
//
//        byte[] cphData = Mapper.INSTANCE.writeValue(cph);
//
//        SigStructure sigStructure = new SigStructure();
//        sigStructure.setContext("Signature1");
//        sigStructure.setProtectedBody(cphData);
//        sigStructure.setExternalData(new byte[0]);
//        sigStructure.setPayload(payload);
//
//        byte[] sigData = Mapper.INSTANCE.writeValue(sigStructure);
//
//        try {
//
//            String algName = finder.getSignatureAlgorithm(ownerKey.getType(),
//                    finder.getKeySizeType(publicKey));
//            Signature sig = Signature.getInstance(algName, getProvider());
//            sig.initSign(signingKey);
//            sig.update(sigData);
//            byte[] derSign = sig.sign();
//            byte[] finalSign = derSign;
//            if (publicKey instanceof ECKey) {
//                // COSE ECDSA signatures are not DER, but are instead R|S, with R and S padded to
//                // key length and concatenated.  We must convert.
//                BigInteger r;
//                BigInteger s;
//                try (ByteArrayInputStream bin = new ByteArrayInputStream(derSign);
//                     ASN1InputStream in = new ASN1InputStream(bin)) {
//
//                    DLSequence sequence = (DLSequence) in.readObject();
//                    r = ((ASN1Integer) sequence.getObjectAt(0)).getPositiveValue();
//                    s = ((ASN1Integer) sequence.getObjectAt(1)).getPositiveValue();
//                }
//
//                // PKCS11 keys cannot be directly interrogated,
//                //guess key size from associated algorithm IDs
//                final int size;
//                switch (ownerKey.getType()) {
//                    case SECP256R1:
//                        size = 32;
//                        break;
//                    case SECP384R1:
//                        size = 48;
//                        break;
//                    default:
//                        throw new InvalidParameterException("coseSignatureAlg "
//                        + ownerKey.getType());
//                }
//                finalSign = new byte[2 * size];
//                BufferUtils.writeBigInteger(r, finalSign, 0, size);
//                BufferUtils.writeBigInteger(s, finalSign, size, size);
//            }
//
//            CoseSign1 sign1 = new CoseSign1();
//            sign1.setProtectedHeader(cphData);
//            sign1.setUnprotectedHeader(new CoseUnprotectedHeader());
//            sign1.setPayload(payload);
//            sign1.setSignature(finalSign);
//
//            return sign1;
//
//        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
//            throw new IOException(e);
//        }
//    }
//
//    @Override
//    public boolean verify(CoseSign1 message, OwnerPublicKey ownerKey) throws IOException {
//        SigStructure sigStructure = new SigStructure();
//        sigStructure.setContext("Signature1");
//        sigStructure.setProtectedBody(message.getProtectedHeader());
//        sigStructure.setExternalData(new byte[0]);
//        sigStructure.setPayload(message.getPayload());
//
//        byte[] sigData = Mapper.INSTANCE.writeValue(sigStructure);
//        byte[] derSig = message.getSignature();
//        byte[] maroePrefix = message.getUnprotectedHeader().getMaroPrefix();
//
//        PublicKey publicKey = decodeKey(ownerKey);
//        if (publicKey instanceof ECKey) {
//            // The encoded signature is fixed-width r|s concatenated, we must convert it to DER.
//            int size = message.getSignature().length / 2;
//            ASN1Integer r =
//                    new ASN1Integer(new BigInteger(1, message.getSignature(), 0, size));
//            ASN1Integer s =
//                    new ASN1Integer(new BigInteger(1, message.getSignature(), size, size));
//            DLSequence sequence = new DLSequence(new ASN1Encodable[]{r, s});
//            ByteArrayOutputStream sigBytes = new ByteArrayOutputStream();
//            ASN1OutputStream asn1out = ASN1OutputStream.create(sigBytes);
//            asn1out.writeObject(sequence);
//            byte[] b = sigBytes.toByteArray();
//            derSig = Arrays.copyOf(b, b.length);
//
//            // if MAROE based signature - prefix signature data with maroe prefix
//            if (maroePrefix != null && maroePrefix.length > 0) {
//                try {
//                    ByteArrayOutputStream bas = new ByteArrayOutputStream();
//                    bas.write(maroePrefix);
//                    bas.write(sigData);
//                    sigData = bas.toByteArray();
//                } catch (IOException ex) {
//                    // should never get here
//                    return false;
//                }
//            }
//        }
//
//        try {
//            AlgorithmFinder finder = new AlgorithmFinder();
//
//            String algName = finder.getSignatureAlgorithm(ownerKey.getType(),
//                    finder.getKeySizeType(publicKey));
//
//            Signature sig = Signature.getInstance(algName, getProvider());
//            sig.initVerify(publicKey);
//            sig.update(sigData);
//            return sig.verify(derSig);
//
//        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
//            throw new IOException(e);
//        }
//    }
//
//    @Override
//    public boolean verify(CoseSign1 message, SigInfo sigInfo) throws IOException {
//        SigStructure sigStructure = new SigStructure();
//        sigStructure.setContext("Signature1");
//        sigStructure.setProtectedBody(message.getProtectedHeader());
//        sigStructure.setExternalData(new byte[0]);
//        sigStructure.setPayload(message.getPayload());
//
//        byte[] signature = message.getSignature();
//        byte[] sigData = Mapper.INSTANCE.writeValue(sigStructure);
//        byte[] maroePrefix = message.getUnprotectedHeader().getMaroPrefix();
//
//        SigInfoType sigInfoType = sigInfo.getSigInfoType();
//        byte[] groupId = sigInfo.getInfo();
//
//        EpidService epidService = new EpidService();
//        if (!epidService.verifyEpidSignature(
//                signature,
//                maroePrefix,
//                message.getUnprotectedHeader().getEatNonce().getNonce(),
//                sigData,
//                groupId,
//                sigInfoType)) {
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public KexMessage getKeyExchangeMessage(String kexSuiteName,
//    KexParty party, OwnerPublicKey ownerKey) throws IOException {
//        return null;
//    }
//
//    @Override
//    public KeyExchangeResult getSharedSecret(String suiteName,
//    byte[] message, KexMessage ownState, Key decryptionKey) throws IOException {
//        return null;
//    }
//
//    @Override
//    public EncryptionState getEncryptionState(KeyExchangeResult kxResult,
//    CipherSuiteType cipherType) throws IOException {
//        return null;
//    }
//
//    @Override
//    public byte[] encrypt(byte[] payload, EncryptionState state) throws IOException {
//        return new byte[0];
//    }
//
//    @Override
//    public byte[] decrypt(byte[] message, EncryptionState state) throws IOException {
//        return new byte[0];
//    }
//
//    @Override
//    public void destroyKey(KeyPair pair) {
//        if (pair != null) {
//            destroyKey(pair.getPrivate());
//        }
//    }
//
//    @Override
//    public void destroyKey(PrivateKey privateKey) {
//        if (privateKey != null && !privateKey.isDestroyed()) {
//            try {
//                privateKey.destroy();
//            } catch (DestroyFailedException e) {
//                //crypto lib does not support destroy
//            }
//        }
//    }
//
//    @Override
//    public void destroyKey(SecretKey key) {
//        if (key != null && !key.isDestroyed()) {
//            try {
//                key.destroy();
//            } catch (DestroyFailedException e) {
//                //crypto lib does not support destroy
//            }
//        }
//
//    }
//}
