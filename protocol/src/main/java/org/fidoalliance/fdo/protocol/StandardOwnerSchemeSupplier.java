package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StandardOwnerSchemeSupplier implements OwnerSchemesSupplier {


  @Override
  public List<String> get() throws IOException {
    List<String> schemes = new ArrayList<>();
    schemes.add(HttpUtils.HTTPS_SCHEME);
    return schemes;
  }
}
