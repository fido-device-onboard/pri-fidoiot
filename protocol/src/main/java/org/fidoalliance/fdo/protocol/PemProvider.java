package org.fidoalliance.fdo.protocol;

import java.security.Provider;

public class PemProvider extends Provider {

  /**
   * Constructs a PemProvider.
   */
  public PemProvider() {

    super("FdoPem", "1.0", "Fdo Keystore Pem Provider");
    put("KeyStore.PEM",PemKeyStoreImpl.class.getName());


  }
}
