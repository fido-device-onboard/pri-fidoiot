// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.InvalidGuidException;
import org.fidoalliance.fdo.protocol.serialization.GuidDeserializer;
import org.fidoalliance.fdo.protocol.serialization.GuidSerializer;

@JsonSerialize(using = GuidSerializer.class)
@JsonDeserialize(using = GuidDeserializer.class)
public class Guid {

  private final UUID uuid;

  private Guid(UUID uuid) {
    this.uuid = uuid;
  }

  public static Guid fromUuid(UUID uuid) {
    return new Guid(uuid);
  }

  /**
   * Converts guid from bytes.
   * @param data The bytes of the guid.
   * @return The Guid object represented by the bytes.
   * @throws IOException An error occurred.
   */
  public static Guid fromBytes(byte[] data) throws IOException {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bis)) {
      long mostSig = in.readLong();
      long leastSig = in.readLong();
      return new Guid(new UUID(mostSig, leastSig));
    } catch (IOException e) {
      throw new IOException(new InvalidGuidException(e));
    }
  }

  public UUID toUuid() {
    return uuid;
  }

  /**
   * Converts GUID to bytes.
   * @return The guid as a byte array.
   */
  public byte[] toBytes() {
    byte[] data = new byte[Long.BYTES * 2];
    ByteBuffer buffer = ByteBuffer.wrap(data);
    buffer.putLong(uuid.getMostSignificantBits());
    buffer.putLong(uuid.getLeastSignificantBits());
    return data;
  }


  public static Guid fromRandomUuid() {
    return fromUuid(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return uuid.toString();
  }

  @Override
  public boolean equals(Object o) {

    if (o == null) {
      return false;
    }
    Class theClass = o.getClass();
    if (theClass.isAssignableFrom(Guid.class)) {
      return this.uuid.equals(((Guid) o).uuid);
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
