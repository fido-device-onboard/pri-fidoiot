// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * A parser of serialized ownership vouchers.
 */
public class OwnershipVoucherParser {

  /**
   * Read an OwnershipVoucher from the provided Reader.
   *
   * @param reader the input Reader
   * @return The decoded OwnershipVoucher
   * @throws IOException if the input is invalid, or if a low-level IO error occurs.
   */
  public OwnershipVoucher readObject(Reader reader) throws IOException {
    CharArrayWriter writer = new CharArrayWriter();
    reader.transferTo(writer);
    writer.flush();
    return new OwnershipVoucherCodec.OwnershipProxyDecoder().decode(
      CharBuffer.wrap(writer.toCharArray()));
  }
}
