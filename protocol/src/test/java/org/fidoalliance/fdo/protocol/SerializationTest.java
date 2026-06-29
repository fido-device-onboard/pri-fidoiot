package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.codec.DecoderException;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.cbor.CBORConstants;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationTest {

  @Test
  public void Test() throws DecoderException, IOException {

    Hash hash = new Hash();
    hash.setHashType(HashType.SHA256);
    hash.setHashValue(new byte[] {1,2,3,4});

    byte[] data = Mapper.INSTANCE.writeValue(hash);


    OwnerPublicKey ownerKey = new OwnerPublicKey();
    ownerKey.setType(PublicKeyType.SECP256R1);
    ownerKey.setEnc(PublicKeyEncoding.X509);
    ownerKey.setBody(AnyType.fromObject(new byte[]{1, 2, 3}));

    data = Mapper.INSTANCE.writeValue(ownerKey);
    OwnerPublicKey ownerKey2 = Mapper.INSTANCE.readValue(data, OwnerPublicKey.class);
    data = ownerKey2.getBody().covertValue(byte[].class);

    /*GenericMap map = new GenericMap();
    map.put(-1, -7);
    map.put(-2, new byte[]{1, 2, 3});
    map.put(-3, new byte[]{1, 2, 3});


    CoseKeyBody coseKeyBody = new CoseKeyBody();

    coseKeyBody.getMap().putAll(map);

    ownerKey.setEnc(PublicKeyEncoding.COSEKEY);
    ownerKey.setBody(coseKeyBody);
    data = Mapper.INSTANCE.toBytes(ownerKey);
    str = Hex.encodeHexString(data);
    ownerKey2 = Mapper.INSTANCE.readObject(data, OwnerPublicKey.class);*/
    byte[] expectedData = new byte[]{1,2,3};
    assert (Arrays.equals(expectedData, data));
  }

  @Test
  public void wrapCborTest() throws IOException {
    String[] input = new String[]{"date", "+%s"};

    byte[] outerBstr = Mapper.INSTANCE.writeAsCborByteString(input);

    byte[] innerCbor = Mapper.INSTANCE.readValue(outerBstr, byte[].class);

    assertTrue(CBORConstants.hasMajorType(CBORConstants.MAJOR_TYPE_BYTES, outerBstr[0]));
    assertTrue(CBORConstants.hasMajorType(CBORConstants.MAJOR_TYPE_ARRAY, innerCbor[0]));

    assertArrayEquals(input, Mapper.INSTANCE.readValue(innerCbor, String[].class));
    assertArrayEquals(input, Mapper.INSTANCE.readFromCborByteString(outerBstr, String[].class));
  }

  @Test
  public void cborNullTest() {
      byte[] cbor = Mapper.INSTANCE.cborNull();

      assertTrue(CBORConstants.hasMajorType(CBORConstants.MAJOR_TYPE_MISC, cbor[0]));
      assertEquals(CBORConstants.BYTE_NULL, cbor[0]);
  }
}
