package org.fidoalliance.fdo.protocol;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

  @Test
  void sanityCheckTest() {
    String directive  = "81858205696C6F63616C686F73748203191F68820C018202447F00000182041920FB";
    Composite rvi = Composite.fromObject(directive);
    Boolean check = RendezvousInfoDecoder.sanityCheck(rvi);
    assertTrue(check);
  }

  @Test
  void mulitpleRvSanityTest() {
    String directive  = "82858205696C6F63616C686F73748203191F68820C018202447F00000182041920FB8582" +
            "056B72766C6F63616C686F73748203191F68820C018202440A14010182041920FB";
    Composite rvi = Composite.fromObject(directive);
    Boolean check = RendezvousInfoDecoder.sanityCheck(rvi);
    assertTrue(check);
  }

  @Test
  void invalidRvVariableSanityTest() {
    //contains rvVariable beyond the acceptable range
    String invalidRVvariable = "81858205696C6F63616C686F73748203191F6882181B018202447F00000182041920FB";
    Composite rvi = Composite.fromObject(invalidRVvariable);
    Boolean check = RendezvousInfoDecoder.sanityCheck(rvi);
    assertFalse(check);
  }

  @Test
  void invalidIpAddressSanityTest() {
    //contains invalid ip address.
    String invalidIpBlob = "81858205696C6F63616C686F73748203191F68820C018202457F0000FFFF82041920FB";
    Composite rvi = Composite.fromObject(invalidIpBlob);
    Boolean check = RendezvousInfoDecoder.sanityCheck(rvi);
    assertFalse(check);
  }

  @Test
  void invalidPortSanityTest() {
    //contains invalid port number.
    String invalidPortBlob = "81858205696C6F63616C686F73748203191F68820C018202447F0000FF82041A0001420B";
    Composite rvi = Composite.fromObject(invalidPortBlob);
    Boolean check = RendezvousInfoDecoder.sanityCheck(rvi);
    assertFalse(check);
  }

  @Test
  void invalidProtocolSanityTest() {
    //contains invalid port number.
    String invalidProtBlob = "81858205696C6F63616C686F73748203191F68820C0C8202447F00000182041920FB";
    Composite rvi = Composite.fromObject(invalidProtBlob);
    Boolean check = RendezvousInfoDecoder.sanityCheck(rvi);
    assertFalse(check);
  }
}

