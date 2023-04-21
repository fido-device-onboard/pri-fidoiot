package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.CoseKey;
import org.fidoalliance.fdo.protocol.message.CoseKeyCurveType;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.junit.jupiter.api.Test;

public class OwnerPublicKeyTest {

  String certsPem = ""
      + "-----BEGIN CERTIFICATE-----\n"
      + "MIIBIjCByaADAgECAgkApNMDrpgPU/EwCgYIKoZIzj0EAwIwDTELMAkGA1UEAwwC\n"
      + "Q0EwIBcNMTkwNDI0MTQ0NjQ3WhgPMjA1NDA0MTUxNDQ2NDdaMA0xCzAJBgNVBAMM\n"
      + "AkNBMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELAJwkDKz/BaWq1Wx7PjkR5W5\n"
      + "LLIbamgSZeVNUlyFM/t0sMAxAWbvEbDzKu924TX4as3WVjMmfekysx30PlDGJaMQ\n"
      + "MA4wDAYDVR0TBAUwAwEB/zAKBggqhkjOPQQDAgNIADBFAiEApUGbgjYT0k63AeRA\n"
      + "tPM2i+VnW6ckYaJyvFLuuWw+QUACIE5w0ntjHLbvwmqgwCfh5T6u8exQdCA2g9Hs\n"
      + "u53hKcaS\n"
      + "-----END CERTIFICATE-----\n";

  @Test
  public void Test() throws DecoderException, IOException {

    List<Certificate> certs = PemLoader.loadCerts(certsPem);


    byte[] enc1 = certs.get(0).getPublicKey().getEncoded();
    OwnerPublicKey ownerKey = new OwnerPublicKey();
    ownerKey.setBody(AnyType.fromObject(enc1));
    ownerKey.setEnc(PublicKeyEncoding.X509);
    ownerKey.setType(PublicKeyType.SECP256R1);
    byte[] ec256Data = Mapper.INSTANCE.writeValue(ownerKey);
    String str = Hex.encodeHexString(ec256Data);
    assert (str.startsWith("83"));

    //todo: handle other key types
    CoseKey key = new CoseKey();
    key.setCurve(CoseKeyCurveType.P256EC2);
    key.setX(new byte[] {1,2,3});
    key.setY(new byte[] {3,2,1});

    OwnerPublicKey key3 = new OwnerPublicKey();
    key3.setType(PublicKeyType.SECP256R1);
    key3.setEnc(PublicKeyEncoding.COSEKEY);
    key3.setBody(AnyType.fromObject(key));
    byte[] data = Mapper.INSTANCE.writeValue(key3);

    str= Hex.encodeHexString(data);
    assert (str.equals("830a03a3200121430102032243030201"));

  }
}
