package org.sdo.cri.cbor;

/**
 * CBOR common tags, as described in RFC7049.
 */
abstract class Tag {

  static final int DATETIME_STRING_RFC4287 = 0;
  static final int EPOCH_DATETIME = 1;
  static final int POSITIVE_BIGNUM = 2;
  static final int NEGATIVE_BIGNUM = 3;
  static final int DECIMAL_FRACTION = 4;
  static final int BIGFLOAT = 5;
  static final int EXPECTED_BASE64URL_RFC4648 = 21;
  static final int EXPECTED_BASE64_RFC4648 = 22;
  static final int EXPECTED_BASE16_RFC4648 = 23;
  static final int EMBEDDED_CBOR = 24;
  static final int URI_RFC3986 = 32;
  static final int BASE64URL_TEXT_RFC4648 = 33;
  static final int BASE64_TEXT_RFC4648 = 34;
  static final int PCRE_REGEX_ECMA262 = 35;
  static final int MIME_RFC2045 = 36;
  static final int SELF_DESCRIBE_CBOR = 55799;
}

