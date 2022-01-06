package org.fidoalliance.fdo.protocol;


import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class Test {


  private static String sampleOwnerKeyPemEC256 = "-----BEGIN CERTIFICATE-----\n"
      + "MIIB9DCCAZmgAwIBAgIJANpFH5JBylZhMAoGCCqGSM49BAMCMGoxJjAkBgNVBAMM\n"
      + "HVNkbyBEZW1vIE93bmVyIFJvb3QgQXV0aG9yaXR5MQ8wDQYDVQQIDAZPcmVnb24x\n"
      + "EjAQBgNVBAcMCUhpbGxzYm9ybzELMAkGA1UEBhMCVVMxDjAMBgNVBAoMBUludGVs\n"
      + "MCAXDTE5MTAxMDE3Mjk0NFoYDzIwNTQxMDAxMTcyOTQ0WjBqMSYwJAYDVQQDDB1T\n"
      + "ZG8gRGVtbyBPd25lciBSb290IEF1dGhvcml0eTEPMA0GA1UECAwGT3JlZ29uMRIw\n"
      + "EAYDVQQHDAlIaWxsc2Jvcm8xCzAJBgNVBAYTAlVTMQ4wDAYDVQQKDAVJbnRlbDBZ\n"
      + "MBMGByqGSM49AgEGCCqGSM49AwEHA0IABFlVBNhtBi8vLHJgDskMoXAYhf30lHd4\n"
      + "vzoO1w0oYiW9iLGwmUkardXpNeSG3giOc+wR3mthmRoGiut3Mg9eYDSjJjAkMBIG\n"
      + "A1UdEwEB/wQIMAYBAf8CAQEwDgYDVR0PAQH/BAQDAgIEMAoGCCqGSM49BAMCA0kA\n"
      + "MEYCIQDrb3b3tigiReIsF+GiImVKJuBsjU6z8mOtlNyfAr7LPAIhAPOl6TaXaasL\n"
      + "vgML12FQQDT502S6PQPxmB1tRrV2dp8/\n"
      + "-----END CERTIFICATE-----\n"
      + "-----BEGIN EC PRIVATE KEY-----\n"
      + "MHcCAQEEIHg45vhXH9m2SdzNxU55cp94yb962JoNn8F9Zpe6zTNqoAoGCCqGSM49\n"
      + "AwEHoUQDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSUd3i/Og7XDShiJb2IsbCZSRqt\n"
      + "1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
      + "-----END EC PRIVATE KEY-----";

  private static String priMessage = "";
  private static String coseMessage = "";

  public static void main(String[] args) {



    PrivateKey pvtKey = getPrivateKey();
    signMessage2(pvtKey, "hello world");
    signAMessage(pvtKey, "hello world");
    verifyMessage();


    //TargetModelCbor value = mapper.readValue(path.toFile(), TargetModelCbor.class);
  }

  public static void verifyMessage() {
   /* CryptoService cs = new CryptoService();
    try {
      boolean bOk = cs.verify(getPublicKey(),Composite.fromObject(Hex.decodeHex(coseMessage)),null,null,null);
      cs.toString();
    } catch (DecoderException e) {
      e.printStackTrace();
    }*/
  }

  public static PublicKey getPublicKey() {
    try {
      StringReader strReader = new StringReader(sampleOwnerKeyPemEC256);
      JcaX509CertificateConverter jcac = new JcaX509CertificateConverter();
      PEMParser pemParser = new PEMParser(strReader);
      for (; ; ) {
        Object o = pemParser.readObject();
        if (o == null) {
          break;
        }
        if (o instanceof X509CertificateHolder) {
          X509CertificateHolder holder = (X509CertificateHolder) o;
          return jcac.getCertificate(holder).getPublicKey();
        }
      }
    } catch (IOException | CertificateException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return null;
  }

  public static PrivateKey getPrivateKey() {
    try {
      StringReader strReader = new StringReader(sampleOwnerKeyPemEC256);
      JcaX509CertificateConverter jcac = new JcaX509CertificateConverter();
      PEMParser pemParser = new PEMParser(strReader);
      for (; ; ) {
        Object o = pemParser.readObject();
        if (o == null) {
          break;
        }
        if (o instanceof PEMKeyPair) {
          PEMKeyPair kp = (PEMKeyPair) o;
          return new JcaPEMKeyConverter().getPrivateKey(kp.getPrivateKeyInfo());

        } else if (o instanceof PrivateKeyInfo) {
          return new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) o);
        }

      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return null;
  }


  public static void signMessage2(PrivateKey privateKey, String text) {
    /*try {

     CryptoService cs = new CryptoService();
      PublicKey publickKey = getPublicKey();

      int id = cs.getCoseAlgorithm(publickKey);
      Composite msg = cs.sign(privateKey, text.getBytes(StandardCharsets.UTF_8), id);
      String cborStr = Hex.encodeHexString(msg.toBytes());
      priMessage = cborStr;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }*/


  }

  public static void signAMessage(PrivateKey privateKey, String text) {
   /* Sign1Message sign1 = new Sign1Message(true, true);

    PublicKey publicKey = getPublicKey();

    try {
      OneKey key = new OneKey(publicKey, privateKey);
      sign1.addAttribute(HeaderKeys.Algorithm, AlgorithmID.ECDSA_256.AsCBOR(), Attribute.PROTECTED);


      sign1.SetContent(text.getBytes(StandardCharsets.UTF_8));
      //AlgorithmID.
      //A10126 {1,-7}
      //A0 map

      sign1.sign(key);
      byte[] cbor = sign1.EncodeToBytes();

      String cborStr = Hex.encodeHexString(cbor);
      coseMessage = cborStr;
      cborStr.toString();

      Message m = Message.DecodeFromBytes(Hex.decodeHex(priMessage),MessageTag.Sign1);

      Sign1Message sm = (Sign1Message)m;


      if (sm.validate(new OneKey(publicKey, null))) {
        boolean bok = true;
        sm.toString();
      }


    } catch (CoseException | DecoderException e) {
      e.printStackTrace();
    }

  }
  //  Create the signed message
  // SignMessage msg = new SignMessage();
  //  Add the content to the message
  // msg.SetContent(ContentToSign);
  //  Create the signer for the message
  //  Signer signer = new Signer();
  // signer.setKey(signingKey);
  // signer.addAttribute(HeaderKeys.Algorithm, AlgorithmID.AsCBOR(), Attribute.ProtectedAttributes);

  //  msg.addSigner(signer);

  //  Force the message to be signed
  // msg.sign();

  //  Now serialize out the message
  //return msg.EncodeToBytes();
  */
  }
}
