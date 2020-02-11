// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

/**
 * A CharSequence presenting a file as a base64-encoded buffer.
 */
class FileBase64Sequence implements CharSequence {

  // base64 is a 3:4 transform
  private static final int B64_BYTES_PER_BLOCK = 3;
  private static final int B64_CHARS_PER_BLOCK = 4;

  private FileChannel channel = null;
  private final int length;
  private final int offset;
  private final Path path;

  /**
   * Constructor.
   */
  FileBase64Sequence(Path path) {
    this.path = path;
    offset = 0;

    final int fileLength = Long.valueOf(getPath().toFile().length()).intValue();
    int numBlocks = fileLength / B64_BYTES_PER_BLOCK;
    if (0 != fileLength % B64_BYTES_PER_BLOCK) {
      ++numBlocks;
    }
    length = numBlocks * B64_CHARS_PER_BLOCK;
  }

  /**
   * Constructor.
   */
  private FileBase64Sequence(Path path, int offset, int length) {
    this.path = path;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public char charAt(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return new FileBase64Sequence(getPath(), start, end - start);
  }

  @Override
  public String toString() {

    final int firstBlock = getOffset() / B64_CHARS_PER_BLOCK;
    final int lastBlock = (getOffset() + length()) / B64_CHARS_PER_BLOCK + 1;
    final int nSourceBytes = (lastBlock - firstBlock) * B64_BYTES_PER_BLOCK;

    final ByteBuffer bbuf = ByteBuffer.allocate(nSourceBytes);

    try {
      getFileChannel().position(firstBlock * B64_BYTES_PER_BLOCK);
      getFileChannel().read(bbuf);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    bbuf.flip();
    final CharBuffer cbuf = SdoSys.CHARSET.decode(Base64.getEncoder().encode(bbuf));
    cbuf.position(getOffset() % B64_CHARS_PER_BLOCK);
    cbuf.limit(cbuf.position() + length());
    return cbuf.toString();
  }

  private FileChannel getFileChannel() throws IOException {
    if (null == channel) {
      channel = FileChannel.open(getPath(), StandardOpenOption.READ);
    }
    return channel;
  }

  private int getOffset() {
    return offset;
  }

  private Path getPath() {
    return path;
  }
}
