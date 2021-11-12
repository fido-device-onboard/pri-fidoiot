package org.fidoalliance.fdo.protocol.cbor;

import java.util.List;


/**
 * CoseSign1 Tagged wrapper.
 */
public class CoseSign1 extends CoseItem {

  /**
   * Construct a wrapped CoseSign1 Object.
   *
   * @param cose A CoseSign1 Object.
   */
  public CoseSign1(Object cose) {
    super((List<Object>) cose);
  }


}
