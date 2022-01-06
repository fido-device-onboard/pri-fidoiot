package org.fidoalliance.fdo.protocol.serialization;

import org.fidoalliance.fdo.protocol.message.CborTags;

public interface TaggedItem {
  CborTags getTag();
}
