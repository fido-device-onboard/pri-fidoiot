package org.fidoalliance.fdo.protocol.message;

import org.fidoalliance.fdo.protocol.serialization.TaggedItem;

public class CoseSign1 extends CoseItem implements TaggedItem {
  @Override
  public CborTags getTag() {
    return CborTags.COSE_SIGN_1;
  }
}
