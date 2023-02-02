package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.junit.jupiter.api.Test;

public class HashTest {
  private static final LoggerService logger = new LoggerService(HashTest.class);
  @Test
  public void Test() throws DecoderException, IOException {

    Hash hash1 = new Hash();
    hash1.setHashType(HashType.SHA256);
    hash1.setHashValue(new byte[] {1,2,3});


    try {
      byte[] data = Mapper.INSTANCE.writeValue(hash1);
      String str = Hex.encodeHexString(data);
    } catch (MismatchedInputException | InvalidDefinitionException e){
      logger.debug(e.getMessage());
    }

    try {
      byte[] data = new byte[0];
      Hash hash2 = Mapper.INSTANCE.readValue(data,Hash.class);
      assertTrue(hash1.equals(hash2));
    } catch (MismatchedInputException e){
      logger.debug(e.getMessage());
    }


  }
}
