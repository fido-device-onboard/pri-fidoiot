// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;


/**
 * Singleton Pattern for Mapping object.
 * <p>ObjectMapper is thread-safe after configuration</p>
 */
public enum Mapper {

  INSTANCE;
  private final ObjectMapper cborMapper;
  private final ObjectMapper yamlMapper;
  private final ObjectMapper jsonMapper;

  Mapper() {
    cborMapper = new ObjectMapper(new CBORFactory());
    yamlMapper = new ObjectMapper(new YAMLFactory());
    yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    jsonMapper = new ObjectMapper();
  }


  private void writeDiagnostic(JsonNode node, StringBuilder output) throws IOException {

    if (node.isBinary()) {
      output.append("h'");
      output.append(Hex.encodeHexString(node.binaryValue(), false));
      output.append("'");
    } else if (node.isArray()) {
      output.append("[");
      String comma = "";
      int count = node.size();
      for (int i = 0; i < count; i++) {
        output.append(comma);
        writeDiagnostic(node.get(i), output);
        comma = ", ";
      }
      output.append("]");
    } else if (node.isObject()) {
      output.append("{");
      String comma = "";
      Iterator<String> names = node.fieldNames();
      while (names.hasNext()) {
        output.append(comma);
        String fieldName = names.next();
        if (NumberUtils.isCreatable(fieldName)) {
          output.append(NumberUtils.toLong(fieldName));
        } else {
          output.append(fieldName);
        }
        output.append(": ");
        writeDiagnostic(node.get(fieldName), output);
        comma = ", ";
      }
      output.append("}");
    } else if (node.isTextual()) {
      output.append("\"");

      output.append(StringEscapeUtils.escapeJson(node.asText()));
      output.append("\"");
    } else if (node.isNumber()) {
      output.append(node.asLong());
    } else if (node.isBoolean()) {
      if (node.asBoolean()) {
        output.append("true");
      } else {
        output.append("false");
      }
    } else if (node.isNull()) {
      output.append("null");
    }
  }

  /**
   * Writes an Object as a CBOR Diagnostic encoded string.
   *
   * @param builder The String builder to append to
   * @param value   The object to encode.
   * @return The object representation in Diagnostic form.
   * @throws IOException An error occurred when writing the content.
   */
  public String writeDiagnostic(StringBuilder builder, Object value) throws IOException {
    JsonNode node = cborMapper.valueToTree(value);

    writeDiagnostic(node, builder);

    return builder.toString();
  }

  /**
   * Writes an object as CBOR encoded Bytes.
   *
   * @param value The object to encode.
   * @return The object represented in cbor.
   * @throws IOException An error occurred when writing the value.
   */
  public byte[] writeValue(Object value) throws IOException {
    ObjectWriter writer = cborMapper.writerFor(value.getClass());
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      writer.writeValue(out, value);
      return out.toByteArray();
    }
  }

  /**
   * Writes an Object as a yaml encoded string.
   *
   * @param value The object to encode.
   * @return The object representation in yaml.
   * @throws IOException An error occurred when writing the content.
   */
  public String writeValueAsString(Object value) throws IOException {
    return yamlMapper.writeValueAsString(value);
  }


  /**
   * Reads Config file.
   *
   * @param file The yaml encoded text.
   * @param t    The target class.
   * @param <T>  The target Class type.
   * @return The converted result.
   * @throws IOException An error occurred when reading the content.
   */
  public <T> T readStringValue(File file, Class<T> t) throws IOException {
    ObjectReader reader = yamlMapper.readerFor(t);
    return reader.readValue(file, t);
  }

  /**
   * Reads a yaml value.
   *
   * @param text The yaml encoded text.
   * @param t    The target class.
   * @param <T>  The target Class type.
   * @return The converted result.
   * @throws IOException An error occurred when reading the content.
   */
  public <T> T readValue(String text, Class<T> t) throws IOException {
    ObjectReader reader = yamlMapper.readerFor(t);
    return reader.readValue(text, t);
  }

  /**
   * Reads a cbor value from an input stream.
   *
   * @param in  The input stream.
   * @param t   The target class.
   * @param <T> The target Class type.
   * @return The converted result.
   * @throws IOException An error occurred when reading the content.
   */
  public <T> T readValue(InputStream in, Class<T> t) throws IOException {
    ObjectReader reader = cborMapper.readerFor(t);
    return reader.readValue(in, t);
  }

  /**
   * Reads a value from binary (CBOR) encoding.
   *
   * @param bytes The cbor encoded bytes.
   * @param t     The target class.
   * @param <T>   The target Class type.
   * @return The converted result.
   * @throws IOException An error occurred when reading the content.
   */
  public <T> T readValue(byte[] bytes, Class<T> t) throws IOException {
    ObjectReader reader = cborMapper.readerFor(t);
    return reader.readValue(bytes, t);
  }

  /**
   * Reads a Json encoded value.
   *
   * @param json Json String.
   * @param t    The target class.
   * @param <T>  The target Class type.
   * @return The converted result.
   * @throws IOException An error occurred when reading the content.
   */
  public <T> T readJsonValue(String json, Class<T> t) throws IOException {
    ObjectReader reader = jsonMapper.readerFor(t);
    return reader.readValue(json, t);
  }

  /**
   * Writes an Object as a json encoded string.
   *
   * @param value The object to encode.
   * @return The object representation in json.
   * @throws IOException An error occurred when writing the content.
   */
  public String writeJsonValue(Object value) throws IOException {
    return jsonMapper.writeValueAsString(value);
  }


  /**
   * Threads the cbor content to a JsonNode.
   *
   * @param content The bytes to read.
   * @return A parsed Json that can be converted to a type later.
   * @throws IOException An error occurred when reading the content.
   */
  public JsonNode readTree(byte[] content) throws IOException {
    return cborMapper.readTree(content);
  }

  /**
   * coverts a Json to the target reference.
   *
   * @param fromValue The JsonNode to covert.
   * @param t         The targeted type reference.
   * @return The converted result.
   */
  public <T> T covertValue(JsonNode fromValue, TypeReference<Map<Object, Object>> t) {
    return (T) cborMapper.convertValue(fromValue, t);
  }

  /**
   * coverts a Json Object to the target reference.
   *
   * @param fromValue The JsonNode to covert.
   * @param t         The taget Class Type.
   * @return The converted result.
   */
  public <T> T covertValue(JsonNode fromValue, Class<T> t) {
    return cborMapper.convertValue(fromValue, t);
  }

  /**
   * Coverts an instance to a Json Node.
   *
   * @param fromValue the instance to convert.
   * @param <T>       The target Json Object.
   * @return The Json Object.
   */
  public <T> T valueToTree(Object fromValue) {
    return (T) cborMapper.valueToTree(fromValue);
  }
}
