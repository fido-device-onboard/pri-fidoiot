// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import COSE.AlgorithmID;
import COSE.Attribute;
import COSE.CoseException;
import COSE.HeaderKeys;
import COSE.Message;
import COSE.MessageTag;
import COSE.OneKey;
import COSE.Sign1Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.junit.jupiter.api.Test;

public class CoseEcTest {


  private static final String sampleOwnerKeyPemEC256 = "-----BEGIN CERTIFICATE-----\n"
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


  private String priSignMessage(String text) throws IOException {

    byte[] payload = text.getBytes(StandardCharsets.UTF_8);

    PrivateKey privateKey = PemLoader.loadPrivateKey(sampleOwnerKeyPemEC256, null);

    List<Certificate> certs = PemLoader.loadCerts(sampleOwnerKeyPemEC256);

    CryptoService cs = new StandardCryptoService();
    OwnerPublicKey ownerKey = cs.encodeKey(PublicKeyType.SECP256R1, PublicKeyEncoding.X509,
        certs.toArray(Certificate[]::new));

    byte[] cbor = new byte[100];
    if (privateKey != null) {
      CoseSign1 sign1 = cs.sign(payload, privateKey, ownerKey);
      cbor = Mapper.INSTANCE.writeValue(sign1);
    }
    return Hex.encodeHexString(cbor);

  }


  private void priVerifyMessage(String cborString) throws DecoderException, IOException {
    CoseSign1 sign1 = Mapper.INSTANCE.readValue(Hex.decodeHex(cborString), CoseSign1.class);

    List<Certificate> certs = PemLoader.loadCerts(sampleOwnerKeyPemEC256);

    CryptoService cs = new StandardCryptoService();
    OwnerPublicKey ownerKey = cs.encodeKey(PublicKeyType.SECP256R1, PublicKeyEncoding.X509,
        certs.toArray(Certificate[]::new));

    assertTrue(cs.verify(sign1, ownerKey));


  }

  private void coseVerifyMessage(String cborString)
      throws DecoderException, CoseException, SignatureException {
    Message m = Message.DecodeFromBytes(Hex.decodeHex(cborString), MessageTag.Sign1);

    Sign1Message sm = (Sign1Message) m;

    PublicKey publicKey = PemLoader.loadPublicKeys(sampleOwnerKeyPemEC256).get(0);

    assertTrue(sm.validate(new OneKey(publicKey, null)));
  }

  private String coseSignMessage(String text) throws CoseException, SignatureException {
    Sign1Message sign1 = new Sign1Message(true, true);

    PrivateKey privateKey = PemLoader.loadPrivateKey(sampleOwnerKeyPemEC256, null);
    PublicKey publicKey = PemLoader.loadPublicKeys(sampleOwnerKeyPemEC256).get(0);

    OneKey key = new OneKey(publicKey, privateKey);
    sign1.addAttribute(HeaderKeys.Algorithm, AlgorithmID.ECDSA_256.AsCBOR(), Attribute.PROTECTED);

    sign1.SetContent(text.getBytes(StandardCharsets.UTF_8));

    sign1.sign(key);
    byte[] cbor = sign1.EncodeToBytes();

    return Hex.encodeHexString(cbor);

  }

  @Test
  public void Test() throws DecoderException, IOException, CoseException, SignatureException {

    String cose1 = coseSignMessage("hello word");
    coseVerifyMessage(cose1);
    priVerifyMessage(cose1);
    String pri1 = priSignMessage("hello word");
    priVerifyMessage(pri1);
    coseVerifyMessage(pri1);

  }
}
