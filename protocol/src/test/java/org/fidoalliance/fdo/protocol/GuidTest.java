package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.UUID;
import org.apache.commons.codec.DecoderException;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.input.EOFException;

public class GuidTest {
  @Test
  public void Test() throws DecoderException, IOException {

    Guid guid1 = Guid.fromRandomUuid();
    String str = guid1.toString();

    byte[] data = guid1.toBytes();
    Guid guid2 = null;
    try{
      guid2 = Guid.fromBytes(data);
    } catch (EOFException e){
      assert(false);
    }

    assertTrue(guid1.equals(guid2));

    Guid guid3 = null;
    try{
      guid3  = Guid.fromUuid(UUID.randomUUID());
    } catch (IllegalArgumentException e){
      assert(false);
    }

    assertFalse(guid1.equals(guid3));

  }
}