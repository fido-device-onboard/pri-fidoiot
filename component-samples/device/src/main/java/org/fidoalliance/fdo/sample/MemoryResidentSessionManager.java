package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.fidoalliance.fdo.protocol.InvalidJwtTokenException;
import org.fidoalliance.fdo.protocol.dispatch.SessionManager;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;

/**
 * Provides a Memory Resident Session Manager.
 */
public class MemoryResidentSessionManager implements SessionManager {

  private final Map<String,SimpleStorage> sessions = new HashMap<>();

  @Override
  public SimpleStorage getSession(String name) throws IOException {

    SimpleStorage result =  sessions.get(name);

    if (result == null) {
      throw new InvalidJwtTokenException(name);
    }
    return result;
  }

  @Override
  public void saveSession(String name,SimpleStorage storage) {
    sessions.put(name,storage);
  }


  @Override
  public void expireSession(String name) {
    sessions.remove(name);
  }

}
