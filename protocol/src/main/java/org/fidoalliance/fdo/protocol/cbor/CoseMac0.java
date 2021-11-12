package org.fidoalliance.fdo.protocol.cbor;

import java.util.List;

/**
 * CoseMac0 Tag Wrapper.
 */
public class CoseMac0 extends CoseItem {


  /**
   * Construct a wrapped CoseMac0 Object.
   *
   * @param cose A CoseMac0 Object.
   */
  public CoseMac0(Object cose) {
    super((List<Object>) cose);
  }


}
