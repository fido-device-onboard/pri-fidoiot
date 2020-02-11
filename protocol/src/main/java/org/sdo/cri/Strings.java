// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Json.QUOTE;
import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.nio.CharBuffer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class Strings {

  private static final int ASCII_PRINTABLE_MAX = 0x7e;
  private static final int ASCII_PRINTABLE_MIN = 0x20;
  private static final char ESCAPE = '\\';
  // Per protocol specification, some extra ascii-printable characters must be unicode escaped.
  private static final Set<Character> EXTRA_ESCAPED_CHARS = new HashSet<>(
      Arrays.asList('"', '[', ']', '{', '}', '\\', '&'));
  private static final int NUM_UNICODE_ESCAPE_HEX_DIGITS = 4;
  private static final int RADIX_HEX = 16;

  private static boolean isAsciiPrintable(int c) {
    return c >= ASCII_PRINTABLE_MIN && c <= ASCII_PRINTABLE_MAX;
  }

  private static boolean mustEscape(char c) {
    return !isAsciiPrintable(c) || EXTRA_ESCAPED_CHARS.contains(c);
  }

  /**
   * Encode a string according to the rules laid out in
   * the SDO Protocol Specification.
   */
  public static String encode(String s) {

    StringBuilder builder = new StringBuilder();

    builder.append(QUOTE);

    StringCharacterIterator it;
    try {
      it = new StringCharacterIterator(s);
    } catch (Exception e) {
      it = new StringCharacterIterator("");
    }

    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {

      if (mustEscape(c)) {
        builder.append(
            String.format(ESCAPE + "u%0" + NUM_UNICODE_ESCAPE_HEX_DIGITS + "x", (int) c));

      } else {
        builder.append(c);
      }
    }

    builder.append(QUOTE);
    return builder.toString();
  }

  /**
   * Decode an SDO-encoded string.
   */
  public static String decode(CharBuffer s) throws IOException {

    StringBuilder builder = new StringBuilder();

    expect(s, QUOTE);

    char c;
    while (QUOTE != (c = s.get())) {

      if (ESCAPE == c) { // start of a unicode escape
        c = s.get();

        if ('u' != c) { // \ must be followed by u
          throw new IOException("illegal escape");
        }
        CharBuffer buf = CharBuffer.allocate(NUM_UNICODE_ESCAPE_HEX_DIGITS);

        if (NUM_UNICODE_ESCAPE_HEX_DIGITS != s.read(buf)) {
          throw new IOException("short escape");
        }
        buf.flip();

        try {
          c = (char) Integer.valueOf(buf.toString(), RADIX_HEX).intValue();

        } catch (NumberFormatException e) {
          throw new IOException(e);
        }
        builder.append(c);

      } else {
        builder.append(c);
      }
    }

    return builder.toString();
  }
}
