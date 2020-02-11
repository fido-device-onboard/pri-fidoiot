// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

class Uuids {

  // clock_seq_hi_and_reserved is octet 8
  private static final int CLOCK_SEQ_HI_RES = 8;
  // time_hi_and_version is octets 6-7
  private static final int TIME_HI_AND_VERSION = 6;

  public static UUID buildRandomUuid() {
    return buildRandomUuid(ThreadLocalRandom.current());
  }

  /**
   * Build a new random UUID.
   */
  private static UUID buildRandomUuid(Random random) {
    // Rules for random UUIDs (from RFC4122):
    //
    // o  Set the two most significant bits (bits 6 and 7) of the
    // clock_seq_hi_and_reserved to zero and one, respectively.
    //
    // o  Set the four most significant bits (bits 12 through 15) of the
    // time_hi_and_version field to the 4-bit version number from
    // Section 4.1.3.
    //
    // o  Set all the other bits to randomly (or pseudo-randomly) chosen
    // values.

    byte[] bytes = new byte[Long.BYTES * 2];

    LongBuffer longs = ByteBuffer.wrap(bytes).asLongBuffer();
    while (longs.hasRemaining()) {
      longs.put(random.nextLong());
    }
    longs.flip();

    bytes[CLOCK_SEQ_HI_RES] = (byte) (bytes[CLOCK_SEQ_HI_RES] | 0x80); // set bit 7
    bytes[CLOCK_SEQ_HI_RES] = (byte) (bytes[CLOCK_SEQ_HI_RES] & 0xbf); // clear bit 6

    // clear the top nibble...
    bytes[TIME_HI_AND_VERSION] = (byte) (bytes[TIME_HI_AND_VERSION] & 0x0f);

    // ...then put 4 (UUID v4, 'random or pseudorandom' in it.
    bytes[TIME_HI_AND_VERSION] = (byte) (bytes[TIME_HI_AND_VERSION] | 0x40);

    return new UUID(longs.get(), longs.get());
  }
}
