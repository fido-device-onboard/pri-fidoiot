package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.UUID;
import org.apache.commons.codec.DecoderException;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.junit.jupiter.api.Test;

public class GuidTest {
  @Test
  public void Test() throws DecoderException, IOException {

    Guid guid1 = Guid.fromRandomUUID();
    String str = guid1.toString();

    byte[] data = guid1.toBytes();
    Guid guid2 = Guid.fromBytes(data);

    assertTrue(guid1.equals(guid2));

    Guid guid3  = Guid.fromUUID(UUID.randomUUID());
    assertFalse(guid1.equals(guid3));

  }
}
