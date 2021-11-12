package org.fidoalliance.fdo.protocol.cbor;

import java.util.List;

/**
 * CoseEnrypt0 wrapper.
 */
public class CoseEncrypt0 extends CoseItem {


  /**
   * Construct a wrapped CoseSign1 Object.
   *
   * @param cose A CoseEncrypt0 Object.
   */
  public CoseEncrypt0(Object cose) {
    super((List<Object>) cose);
  }


}
