// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.fido.iot.protocol.cbor.Decoder;
import org.fido.iot.protocol.cbor.Decoder.Builder;
import org.fido.iot.protocol.cbor.Encoder;

/**
 * Wrapper for composing composite cbor types.
 */
public class Composite {

  private Object items;

  private Composite(Object items) {

    if (items == null) {
      throw new IllegalArgumentException();
    }
    items = resolveType(items);
    if (items instanceof List || items instanceof Map) {
      this.items = items;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Gets number of items the composite contains.
   *
   * @return the number of items the composite contains.
   */
  public int size() {
    if (items instanceof List) {
      return ((List<?>) items).size();
    }
    return ((Map<?, ?>) items).size();
  }

  /**
   * Verifies the max key the composite contains.
   *
   * @param maxKey The key to verify.
   */
  public void verifyMaxKey(int maxKey) {
    if (!isArray() || size() != (maxKey + 1)) {
      throw new MessageBodyException();
    }
  }

  /**
   * Determines if a key is contained in the composite.
   *
   * @param key A key to test if its contained in the composite.
   * @return True if the key exists, otherwise false.
   */
  public boolean containsKey(Object key) {
    if (items == null) {
      return false;
    }
    if (items instanceof List) {
      return ((long) key) < ((List<?>) items).size();
    }
    return ((Map<?, ?>) items).containsKey(key);
  }

  /**
   * Gets the underlying storage object.
   *
   * @return The underlying object.
   */
  public Object get() {
    if (items == null) {
      items = new ArrayList<>();
    }
    return items;
  }

  /**
   * Gets the object stored at the key.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist.
   */
  public Object get(Object key) {

    try {
      if (key instanceof Number) {
        if (items instanceof List) {
          return ((List<?>) items).get(((Number) key).intValue());
        } else if (items instanceof Map) {
          return ((Map<?, ?>) items).get(((Number) key).longValue());
        }
      } else if (key instanceof String) {
        if (items instanceof Map) {
          return ((Map<?, ?>) items).get(key);
        }
      }
    } catch (Exception e) {
      throw new MessageBodyException(e);
    }
    throw new IllegalArgumentException();
  }

  /**
   * Gets a Number Value.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist or is not a Number.
   */
  public Number getAsNumber(Object key) {
    Object obj = get(key);
    if (!(obj instanceof Number)) {
      throw new MessageBodyException(new ClassCastException());
    }

    return (Number) obj;
  }

  /**
   * Gets a Boolean Value.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist or is not a Boolean.
   */
  public boolean getAsBoolean(Object key) {
    Object obj = get(key);
    if (!(obj instanceof Boolean)) {
      throw new MessageBodyException(new ClassCastException());
    }
    return (boolean) obj;
  }

  /**
   * Gets a ByteBuffer value.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist or is not a byte array.
   */
  public ByteBuffer getAsByteBuffer(Object key) {
    Object obj = get(key);
    if (obj instanceof ByteBuffer) {
      return (ByteBuffer) obj;
    } else if (obj instanceof byte[]) {
      return ByteBuffer.wrap((byte[]) obj);
    }
    throw new UnsupportedOperationException();
  }

  /**
   * Gets a Byte Array value.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist or is not a byte array.
   */
  public byte[] getAsBytes(Object key) {
    Object obj = get(key);

    if (obj instanceof byte[]) {
      return (byte[]) obj;
    }

    if (obj instanceof ByteBuffer) {
      return unwrap((ByteBuffer) obj);
    }

    if (obj instanceof String) {
      return ((String) obj).getBytes(StandardCharsets.US_ASCII);
    }

    throw new UnsupportedOperationException();
  }

  /**
   * Gets am Object Map value.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist or is not a Map.
   */
  public Map<Object, Object> getAsMap(Object key) {
    Object obj = get(key);

    if (!(obj instanceof Map)) {
      throw new MessageBodyException(new ClassCastException());
    }

    return (Map<Object, Object>) obj;
  }

  /**
   * Gets a String Value.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist or is not a String.
   */
  public String getAsString(Object key) {
    Object obj = get(key);
    if (!(obj instanceof CharSequence)) {
      throw new MessageBodyException(new ClassCastException());
    }
    return obj.toString();
  }

  /**
   * Gets a UUID value.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist or is not a UUID.
   */
  public UUID getAsUuid(Object key) {

    byte[] buf = getAsBytes(key);
    ByteArrayInputStream bis = new ByteArrayInputStream(buf);
    DataInputStream in = new DataInputStream(bis);

    try {
      long mostSig = in.readLong();
      long leastSig = in.readLong();
      return new UUID(mostSig, leastSig);
    } catch (IOException e) {
      throw new InvalidGuidException(e);
    }
  }

  /**
   * Gets a nested composite value.
   *
   * @param key The object key.
   * @return The object stored at the key.
   * @throws MessageBodyException If the key does not exist or is not a nested Composite.
   */
  public Composite getAsComposite(Object key) {
    Object obj = get(key);
    if (obj instanceof Composite) {
      return (Composite) obj;
    }
    if (obj instanceof byte[]) {
      if (((byte[]) obj).length == 0) {
        return new Composite(new HashMap<>());
      }
    }
    if (obj instanceof ByteBuffer) {
      if (!((ByteBuffer) obj).hasRemaining()) {
        return new Composite(new HashMap<>());
      }
    }

    return new Composite(obj);
  }

  /**
   * Sets the object stored at the key.
   *
   * @param key The composite key to set.
   * @param value The value to set.
   * @return The modified composite.
   * @throws IllegalArgumentException If the key is not a valid type.
   */
  public Composite set(Object key, Object value) {

    if (!(key instanceof Number || key instanceof String)) {
      throw new IllegalArgumentException();
    }

    if (items instanceof List) {
      if (key instanceof Number) {
        int index = ((Number) key).intValue();
        if (size() <= index) {
          while (size() < index) {
            ((List<Object>) items).add(Const.DEFAULT);
          }
          ((List<Object>) items).add(resolveType(value));
          return this;
        }
        ((List<Object>) items).set(index, resolveType(value));
        return this;
      }
    } else if (items instanceof Map) {
      ((Map<Object, Object>) items).put(key, resolveType(value));
      return this;
    }
    throw new UnsupportedOperationException();
  }

  private Object resolveType(Object value) {

    if (value == null) {
      return null;
    } else if (value.equals(Optional.empty())) {
      return null;
    } else if (value instanceof Composite) {
      return ((Composite) value).get();
    } else if (value instanceof ByteBuffer) {
      return unwrap((ByteBuffer) value);
    } else if (value instanceof Number) {
      return ((Number) value).longValue();
    } else if (value instanceof UUID) {
      ByteBuffer buffer = ByteBuffer.allocate(Const.GUID_SIZE);
      buffer.putLong(((UUID) value).getMostSignificantBits());
      buffer.putLong(((UUID) value).getLeastSignificantBits());
      return unwrap(buffer.flip());
    } else if (value instanceof InetAddress) {
      return ((InetAddress) value).getAddress();
    } else if (value instanceof Certificate) {
      try {
        return ((Certificate) value).getEncoded();
      } catch (CertificateEncodingException e) {
        throw new RuntimeException(e);
      }
    } else if (value instanceof Object[]) {
      List<Object> list = new ArrayList<>();
      for (Object o : ((Object[]) value)) {
        list.add(resolveType(o));
      }
      return list;
    } else if (value instanceof Optional) {
      return resolveType(((Optional<?>) value).get());
    }
    return value;
  }

  /**
   * Coverts the type to a CBOR byte array.
   *
   * @return The Composite type encoded as CBOR byte array.
   */
  public byte[] toBytes() {
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    WritableByteChannel wbc = Channels.newChannel(bao);
    Encoder encoder = new Encoder.Builder(wbc).build();
    try {
      encoder.writeObject(get());
      return bao.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Coverts the type to a CBOR ByteBuffer.
   *
   * @return The Composite type encoded as CBOR Byte Array.
   */
  public ByteBuffer toByteBuffer() {
    return ByteBuffer.wrap(toBytes());
  }

  /**
   * Creates a Composite from an cbor array of bytes.
   *
   * @param buffer The cbor byte buffer.
   * @return A Composite created from the cbor buffer.
   */
  private static Composite fromBytes(byte[] buffer) {

    ByteArrayInputStream in = new ByteArrayInputStream(buffer);

    ReadableByteChannel rbc = Channels.newChannel(in);
    Decoder decoder = new Builder(rbc).build();
    return new Composite(decoder.next());

  }

  /**
   * Converts a Hex String to a byte array.
   *
   * @param value A Hex string.
   * @return The decode hex string.
   */
  public static byte[] decodeHex(String value) {
    int binaryLength = value.length() / 2;

    if (binaryLength * 2 != value.length()) {
      throw new RuntimeException(new InvalidParameterException(value));
    }
    int c = 0;
    byte[] result = new byte[binaryLength];
    for (int i = 0; i < value.length(); i = i + 2) {
      result[c++] = (byte)
          ((Character.getNumericValue(value.charAt(i)) << 4)
              + Character.getNumericValue(value.charAt(i + 1)));
    }
    return result;
  }

  /**
   * Converts a BytesBuffer to a byte array.
   *
   * @param buffer The ByteBuffer to convert.
   * @return The ByteBuffer as a byte array..
   */
  public static byte[] unwrap(ByteBuffer buffer) {
    if (buffer.hasArray()
        && buffer.remaining() == buffer.array().length
        && buffer.position() == 0) {
      return buffer.array();
    }
    byte[] cpy = new byte[(buffer.remaining())];
    buffer.get(cpy);
    return cpy;
  }

  /**
   * Creates a new Composite as an array of items.
   *
   * @return A new Composite array.
   */
  public static Composite newArray() {
    return new Composite(new ArrayList<>());
  }

  /**
   * Creates a new Composite as a map of items.
   *
   * @return A new Composite Map.
   */
  public static Composite newMap() {
    return new Composite(new HashMap<>());
  }

  /**
   * Creates a new Composite from an object source.
   *
   * @param object source object representing cbor data.
   * @return A new Composite representing the object source.
   */
  public static Composite fromObject(Object object) {
    if ((object instanceof Map) || (object instanceof List)) {
      return new Composite(object);
    }

    if (object instanceof ByteBuffer) {
      if (!((ByteBuffer) object).hasRemaining()) {
        return new Composite(new HashMap<Object, Object>());
      }
      return fromBytes(unwrap((ByteBuffer) object));
    }

    if (object instanceof InputStream) {
      ReadableByteChannel rbc = Channels.newChannel((InputStream) object);
      Decoder decoder = new Builder(rbc).build();
      return new Composite(decoder.next());
    }

    if (object instanceof byte[]) {
      if (((byte[]) object).length == 0) {
        return new Composite(new HashMap<Object, Object>());
      }
      return fromBytes((byte[]) object);
    }

    if (object instanceof String) {
      return fromBytes(decodeHex((String) object));
    }

    return new Composite(object);
  }

  /**
   * Determines if the composite is an array.
   *
   * @return True if the composite is an array, otherwise false.
   */
  public boolean isArray() {
    return get() instanceof List;
  }

  /**
   * Clears all the items from the composite.
   */
  public void clear() {
    if (items instanceof List) {
      ((List<Object>) items).clear();
      return;
    }
    ((Map<Object, Object>) items).clear();
  }

  public Composite clone() {
    return Composite.fromObject(toBytes());
  }

  /**
   * Converts bytes to a HEX string.
   *
   * @param bytes The bytes to covert.
   * @return A hex string representing the bytes.
   */
  public static String toString(byte[] bytes) {

    char[] chars = new char[bytes.length * 2];

    int c = 0;
    for (byte b : bytes) {

      int v = b & 0xFF;
      int hi = v >> 4;
      int low = v & 0x0F;
      chars[c++] = Const.HEX_CHARS[hi];
      chars[c++] = Const.HEX_CHARS[low];
    }
    return new String(chars);
  }

  @Override
  public String toString() {
    return toString(toBytes());
  }

}
