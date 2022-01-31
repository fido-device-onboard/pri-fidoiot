package org.fidoalliance.fdo.protocol.message;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.serialization.ProtocolInfoDeserializer;
import org.fidoalliance.fdo.protocol.serialization.ProtocolInfoSerializer;


@JsonSerialize(using = ProtocolInfoSerializer.class)
@JsonDeserialize(using = ProtocolInfoDeserializer.class)
public class ProtocolInfo {

  private static final ProtocolInfo emptyInfo = new ProtocolInfo();

  private String authToken;

  public String getAuthToken() {
    return authToken;
  }

  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  public boolean hasToken() {
    return authToken != null;
  }

  public static ProtocolInfo empty() {
    return emptyInfo;
  }

  public static ProtocolInfo fromRandomUUID() {
    ProtocolInfo info = new ProtocolInfo();
    info.setAuthToken(UUID.randomUUID().toString());
    return info;
  }

  public static ProtocolInfo fromString(String token) {
    ProtocolInfo info = new ProtocolInfo();
    info.setAuthToken(token);
    return info;
  }

  @Override
  public String toString() {
    return authToken;
  }
}
