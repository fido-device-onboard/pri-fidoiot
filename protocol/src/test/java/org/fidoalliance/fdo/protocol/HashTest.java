package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.junit.jupiter.api.Test;

public class HashTest {
  @Test
  public void Test() throws DecoderException, IOException {

    Hash hash1 = new Hash();
    hash1.setHashType(HashType.SHA256);
    hash1.setHashValue(new byte[] {1,2,3});


    byte[] data = Mapper.INSTANCE.writeValue(hash1);

    Hash hash2 = Mapper.INSTANCE.readValue(data,Hash.class);
    assertTrue(hash1.equals(hash2));

  }
}
