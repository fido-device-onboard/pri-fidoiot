package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class RendezvousInfoDecoderTest {

  @Test
  void getHttpDirectivesTest() throws Exception {

    String ownerdirective  = "http://localhost:8040?ipaddress=10.66.244.99&ownerport=8443";
    Composite rvi = RendezvousInfoDecoder.decode(ownerdirective);
    List<String> ownerDir = RendezvousInfoDecoder.getHttpDirectives(rvi, Const.RV_OWNER_ONLY);
    assertTrue(ownerDir.get(0).endsWith(":8443"));

    String devdirective  = "https://localhost:8040?ipaddress=10.66.244.99&ownerport=8443";
    Composite devRVI = RendezvousInfoDecoder.decode(devdirective);
    List<String> devDir =  RendezvousInfoDecoder.getHttpDirectives(devRVI, Const.RV_DEV_ONLY);
    assertTrue(devDir.get(0).endsWith(":8040"));

  }

  @Test
  void invalidDecodeTest() {

    String invalidDirective  = "tcp://invalid:8040?devonly&rvbypass&owneronly&delaysec=10&"
        + "wifipw=amedium=1&wifissid=dsa&userinput=ac&invalid";

    assertThrows(org.fido.iot.protocol.DispatchException.class ,
        ()-> { Composite rvi = RendezvousInfoDecoder.decode(invalidDirective); });

  }

}

