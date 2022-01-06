package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.Log4jProvider;
import org.fidoalliance.fdo.protocol.LogProvider;
import org.fidoalliance.fdo.protocol.LogProviderFactory;


public class StandardLogProvider implements LogProviderFactory {

  @Override
  public LogProvider apply(Class<?> aClass) {
    return new Log4jProvider(aClass);
  }
}
