package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.InvalidGuidException;
import org.fidoalliance.fdo.protocol.serialization.GuidDeserializer;
import org.fidoalliance.fdo.protocol.serialization.GuidSerializer;

@JsonSerialize(using = GuidSerializer.class)
@JsonDeserialize(using = GuidDeserializer.class)
public class Guid {

  private UUID uuid;

  private Guid(UUID uuid) {
    this.uuid = uuid;
  }
  public static Guid fromUUID(UUID uuid) {
    return new Guid(uuid);
  }

  public static Guid fromBytes(byte[] data) throws IOException {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bis);) {
      long mostSig = in.readLong();
      long leastSig = in.readLong();
      return new Guid(new UUID(mostSig, leastSig));
    } catch (IOException e) {
      throw new IOException(new InvalidGuidException(e));
    }
  }

  public static Guid fromRandomUUID() {
    return fromUUID(UUID.randomUUID());
  }

  public UUID toUuid() {
    return uuid;
  }

  public byte[] toBytes() {
    byte[] data = new byte[Long.BYTES * 2];
    ByteBuffer buffer = ByteBuffer.wrap(data);
    buffer.putLong(uuid.getMostSignificantBits());
    buffer.putLong(uuid.getLeastSignificantBits());
    return data;
  }

  @Override
  public String toString() {
    return uuid.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Guid) {
      return this.uuid.equals(((Guid) o).uuid);
    }
    if (o instanceof UUID) {
      return this.uuid.equals(o);
    }
    return super.equals(o);
  }

}
