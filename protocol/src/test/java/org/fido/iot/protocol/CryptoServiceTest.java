// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class CryptoServiceTest {

  private final String ecKey = "-----BEGIN PUBLIC KEY-----\n"
      + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSU\n"
      + "d3i/Og7XDShiJb2IsbCZSRqt1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
      + "-----END PUBLIC KEY-----\n";

  private final String rsakey = "-----BEGIN PUBLIC KEY-----\n"
      + "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCqGKukO1De7zhZj6+H0qtjTkVxwTCpvKe4eCZ0\n"
      + "FPqri0cb2JZfXJ/DgYSF6vUpwmJG8wVQZKjeGcjDOL5UlsuusFncCzWBQ7RKNUSesmQRMSGkVb1/\n"
      + "3j+skZ6UtW+5u09lHNsj6tQ51s1SPrCBkedbNf0Tp0GbMJDyR4e9T04ZZwIDAQAB\n"
      + "-----END PUBLIC KEY-----";

  private static final String VOUCHER = ""
      + "8486186450f0956089c0df4c349c61f460457e87eb8185820567302e302e302e3082024400000000820419cb9"
      + "f820319cb9f820b146a44656d6f446576696365830d0258402c02709032b3fc1696ab55b1ecf8e44795b92cb2"
      + "1b6a681265e54d525c8533fb74b0c0310166ef11b0f32aef76e135f86acdd65633267de932b31df43e50c6258"
      + "22F5820744c9e7af744e7408e288d27017d0904605eed5bc07e7c1771404e569bdfe3e682055820cf2b488f87"
      + "d0af1755d7aeb775879a14bc3d0c5989af0db0dfdf235ed06bfe538259017a308201763082011d0209008da35"
      + "5b7e71c51f5300a06082a8648ce3d040302300d310b300906035504030c0243413020170d3139313132323135"
      + "353430315a180f32303534313131333135353430315a3078310b3009060355040613025553310f300d0603550"
      + "4080c064f7265676f6e3112301006035504070c0948696c6c73626f726f310e300c060355040a0c05496e7465"
      + "6c311d301b060355040b0c14446576696365204d616e75666163747572696e673115301306035504030c0c446"
      + "56d6f44657669636532373059301306072a8648ce3d020106082a8648ce3d03010703420004a582f072ec6a47"
      + "46d8e7c974558a6c4ec694ce91420a978dddb995d201e9e712c7330bc1151c8eb656313745dac7c7040ec7ef2"
      + "2e549621632b5b3863e467c98300a06082a8648ce3d040302034700304402204386077f39aee794f7e48eaf04"
      + "ff4c18822a8c306994ad4ad75ccab5aef7478c022073ce183429452662c29d4c4d1b750f63167e85c9cb0ef7b"
      + "2581a986ec9282bf1590126308201223081c9a003020102020900a4d303ae980f53f1300a06082a8648ce3d04"
      + "0302300d310b300906035504030c0243413020170d3139303432343134343634375a180f32303534303431353"
      + "134343634375a300d310b300906035504030c0243413059301306072a8648ce3d020106082a8648ce3d030107"
      + "034200042c02709032b3fc1696ab55b1ecf8e44795b92cb21b6a681265e54d525c8533fb74b0c0310166ef11b"
      + "0f32aef76e135f86acdd65633267de932b31df43e50c625a310300e300c0603551d13040530030101ff300a06"
      + "082a8648ce3d0403020348003045022100a5419b823613d24eb701e440b4f3368be5675ba72461a272bc52eeb"
      + "96c3e414002204e70d27b631cb6efc26aa0c027e1e53eaef1ec5074203683d1ecbb9de129c692818443a1012680"
      + "588e8382085820b7db8ebbceb119147d28a70ae50de328cdb7d7984ecf147b90d117ac721a6c128208582082d"
      + "4659e9dbbc7fac58ad015faf42ac0947ee511d752ab37edc42eb0d969df28830d025840595504d86d062f2f2c"
      + "72600ec90ca1701885fdf4947778bf3a0ed70d286225bd88b1b099491aadd5e935e486de088e73ec11de6b619"
      + "91a068aeb77320f5e6034584830460221009eb8a5a1d81e3bb69c0f3a6844e280d2af67119ac5e53109a45129"
      + "be247726510221008085c9b2029b5171dd1780a038ce5059fece59d36fb086db6e25adcdedaa9c0c";

  private PublicKey invalidKey = new PublicKey() {
    @Override
    public String getAlgorithm() {
      return null;
    }

    @Override
    public String getFormat() {
      return null;
    }

    @Override
    public byte[] getEncoded() {
      return new byte[0];
    }
  };

  public PublicKey generatePublicKey(String key, String type)
      throws InvalidKeySpecException, NoSuchAlgorithmException {

    String strPublicKey = key.replace("-----BEGIN PUBLIC KEY-----\n", "")
        .replace("-----END PUBLIC KEY-----", "").replace("\n", "");
    byte[] asBytes = Base64.getDecoder().decode(strPublicKey);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(asBytes);
    KeyFactory keyFactory = KeyFactory.getInstance(type);
    return keyFactory.generatePublic(spec);
  }

  public X509Certificate generateX509Certificate(String pem) throws CertificateException, IOException {
    InputStream input = new ByteArrayInputStream(pem.getBytes());
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    Certificate cert = cf.generateCertificate(input);
    return (X509Certificate)cert;
  }

  @Test
  void CryptoConstructorTest() throws Exception {

    CryptoService cs = new CryptoService(new String[]{"TLS_"});
    assert(SecureRandom.getInstanceStrong().toString().
       equals(cs.getSecureRandom().toString()));
  }

  @Test
  void getPublicKeyTypeTest() throws InvalidKeySpecException, NoSuchAlgorithmException {

    CryptoService cs = new CryptoService();
    PublicKey validKey = generatePublicKey(ecKey, "EC");
    int res = cs.getPublicKeyType(validKey);
    assertTrue(res == Const.PK_SECP256R1);

    PublicKey validRsaKey = generatePublicKey(rsakey, "RSA");
    res = cs.getPublicKeyType(validRsaKey);
    assertTrue(res == Const.PK_RSA2048RESTR);

    //Checking the invalid Key scenario
    assertThrows(java.lang.RuntimeException.class, ()-> {
      cs.getPublicKeyType(invalidKey);
    });

  }

  @Test
  void getCompatibleEncodingTest() throws InvalidKeySpecException, NoSuchAlgorithmException {

    CryptoService cs = new CryptoService();
    PublicKey validKey = generatePublicKey(ecKey, "EC");
    int res = cs.getCompatibleEncoding(validKey);
    assertTrue(res == Const.PK_ENC_COSEEC);

    PublicKey validRsaKey = generatePublicKey(rsakey, "RSA");
    res = cs.getCompatibleEncoding(validRsaKey);
    assertTrue(res == Const.PK_ENC_X509);

    //Checking the invalid Key scenario
    assertThrows(java.lang.RuntimeException.class, ()-> {
      cs.getCompatibleEncoding(invalidKey);
    });

  }

  @Test
  void getCompatibleHashTypeTest() throws InvalidKeySpecException, NoSuchAlgorithmException {

    CryptoService cs = new CryptoService();
    PublicKey validKey = generatePublicKey(ecKey, "EC");
    int res = cs.getCompatibleHashType(validKey);
    assertTrue(res == Const.SHA_256);

    //Checking the invalid Key scenario
    assertThrows(java.lang.RuntimeException.class, ()-> {
      cs.getCompatibleHashType(invalidKey);
    });

  }

  @Test
  void encodeTest() throws InvalidKeySpecException, NoSuchAlgorithmException {

    CryptoService cs = new CryptoService();
    PublicKey validKey = generatePublicKey(rsakey, "RSA");
    Composite res = cs.encode(validKey,Const.PK_ENC_CRYPTO);
    assertTrue(res.size() > 0 );

  }

  @Test
  void decodeTest() throws InvalidKeySpecException, NoSuchAlgorithmException {

    CryptoService cs = new CryptoService();
    PublicKey validKey = generatePublicKey(rsakey, "RSA");
    Composite encode = cs.encode(validKey,Const.PK_ENC_CRYPTO);
    PublicKey key =  cs.decode(encode);
    assertTrue(key.getAlgorithm().equals("RSA"));

  }

  @Test
  void verifyAlgorithmTest() {

    CryptoService cs = new CryptoService();
    assertDoesNotThrow( ()-> {
      cs.verifyAlgorithm("EC"); });

    assertThrows(org.fido.iot.protocol.InvalidOwnershipVoucherException.class,
        ()-> { cs.verifyAlgorithm("RSA"); } );

  }

  @Test
  void getFingerPrintTest() throws InvalidKeySpecException, NoSuchAlgorithmException {

    CryptoService cs = new CryptoService();
    PublicKey validKey = generatePublicKey(ecKey, "EC");
    String res = cs.getFingerPrint(validKey);
    assertTrue(res.length() == 32 );

  }

  @Test
  void verifyVoucherTest() throws CertificateException, IOException {

    CryptoService cs = new CryptoService();
    assertDoesNotThrow( ()-> {
    cs.verifyVoucher(Composite.fromObject(VOUCHER)); });
  }
}

