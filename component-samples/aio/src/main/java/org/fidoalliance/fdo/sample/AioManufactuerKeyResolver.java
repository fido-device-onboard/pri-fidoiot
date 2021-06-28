package org.fidoalliance.fdo.sample;

import java.security.PrivateKey;
import java.security.PublicKey;
import org.fidoalliance.fdo.protocol.KeyResolver;

public class AioManufactuerKeyResolver implements KeyResolver {

  @Override
  public PrivateKey getKey(PublicKey key) {
    return null;
  }
}
