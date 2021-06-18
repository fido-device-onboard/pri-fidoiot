package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class RendezvousInfoDecoderTest {

  @Test
  void getHttpDirectivesTest() throws Exception {

    // localhost dnsname or localhost IP address can be replaced by any other IP
    String directive  = "81858205696c6f63616c686f73748203191f68820c018202447f00000182041920fb";

    Composite devRvi = Composite.fromObject(directive);
    List<String> devDir =  RendezvousInfoDecoder.getHttpDirectives(devRvi, Const.RV_DEV_ONLY);
    assertTrue(devDir.get(0).endsWith(":8040"));

  }

  @Test
  void invalidRvInfoTest() {

    String invalidRvInfo  = "81868205696c6f63616c686f73748203191f68820c018202447f00000182041920fb";

    Composite invalidRvi = Composite.fromObject(invalidRvInfo);

    assertThrows(java.lang.IllegalArgumentException.class ,
        ()-> { List<String> directives = RendezvousInfoDecoder.getHttpDirectives(invalidRvi,Const.RV_DEV_ONLY); });
  }

}

