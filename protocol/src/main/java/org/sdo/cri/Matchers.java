// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.nio.CharBuffer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.function.Predicate;

/**
 * Common text-matching utilities.
 */
class Matchers {

  /**
   * Assert that the next character inputs match the expected string.
   *
   * @param in the input text
   * @param match the string which we expect the input text to match
   * @throws IOException if the text doesn't match the target
   */
  public static void expect(CharBuffer in, final String match) throws IOException {

    StringCharacterIterator iter = new StringCharacterIterator(match);
    for (char c = iter.first(); CharacterIterator.DONE != c; c = iter.next()) {
      expect(in, c);
    }
  }

  /**
   * Assert that the next character input matches the expected value.
   *
   * @param in the input text
   * @param match the character which we expect the input text to match
   * @throws IOException if the text doesn't match the target
   */
  public static void expect(CharBuffer in, final Character match) throws IOException {

    expect(in, match::equals);
  }

  /**
   * Assert that the next character input satisfies the supplied predicate.
   *
   * @param in the input text
   * @param predicate the character test which will determine if we have a match
   * @throws IOException if the text doesn't match the target
   */
  private static void expect(CharBuffer in, Predicate<Character> predicate) throws IOException {

    char c = in.get();

    if (!predicate.test(c)) {
      throw new IOException("unexpected input: " + c);
    }
  }

  /**
   * Consume the 'actual' CharBuffer and match it to the 'expected' sequence.
   *
   * @param expected The expected characters
   * @param actual The buffer to match against the expected characters
   *
   * @return true if the buffer contents matched expectations, false otherwise.
   */
  public static boolean consumeMatching(CharSequence expected, CharBuffer actual) {

    int result = CharBuffer.wrap(expected).mismatch(actual);
    if (-1 == result || expected.length() == result) {
      actual.position(actual.position() + expected.length());
      return true;
    } else {
      return false;
    }
  }
}
