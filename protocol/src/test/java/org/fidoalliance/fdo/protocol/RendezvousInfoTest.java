package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.RendezvousDirective;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.fidoalliance.fdo.protocol.message.RendezvousInstruction;
import org.fidoalliance.fdo.protocol.message.RendezvousVariable;
import org.junit.jupiter.api.Test;

public class RendezvousInfoTest {
  private static final LoggerService logger = new LoggerService(RendezvousInfoTest.class);
  @Test
  public void Test() throws DecoderException, IOException {

    RendezvousInfo info = new RendezvousInfo();
    RendezvousDirective dir = new RendezvousDirective();
    RendezvousInstruction ins1 = new RendezvousInstruction();

    Hash hash = new Hash();
    hash.setHashType(HashType.SHA256);
    hash.setHashValue(new byte[] {1,2,3});
    ins1.setVariable(RendezvousVariable.CL_CERT_HASH);
    ins1.setValue(Mapper.INSTANCE.writeValue(hash));
    dir.add(ins1);
    info.add(dir);

    byte[] data = Mapper.INSTANCE.writeValue(info);
    String str = Hex.encodeHexString(data);
    RendezvousInfo rvi = Mapper.INSTANCE.readValue(data,RendezvousInfo.class);


    data = Hex.decodeHex("81858205781F66646F31302E7765737475732E636C6F75646170702E617A7572652E636F6D82031850820C018202448A5BC35582041850");
    rvi = Mapper.INSTANCE.readValue(data,RendezvousInfo.class);
    try {
      str = Mapper.INSTANCE.writeValueAsString(rvi);
    } catch (InvalidDefinitionException i){
      logger.error(i.getMessage());
    }


  }
}
