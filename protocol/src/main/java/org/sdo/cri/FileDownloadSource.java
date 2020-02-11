// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Source (issuer) sdo_sys:(filedesc|write) instructions.
 *
 * <p>sdo_sys:filedesc sets the filename to be written to on the receiving end.
 *
 * <p>sdo_sys:write contains a block of data to be written to the receiving file.
 *
 * <p>Data is encoded as base64-strings.
 */
class FileDownloadSource implements ServiceInfoMultiSource {

  private final Path local;
  private final Path remote;

  private FileDownloadSource(Path local, Path remote) { // use Builder to create instances
    this.local = local;
    this.remote = remote;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public List<Entry<CharSequence, CharSequence>> getServiceInfo(UUID uuid) {
    final List<Entry<CharSequence, CharSequence>> list = new LinkedList<>();

    final Base64.Encoder b64e = Base64.getEncoder();
    final String b64 = b64e.encodeToString(getRemote().toString().getBytes(SdoSys.CHARSET));
    Entry<CharSequence, CharSequence> entry = new SimpleEntry<>(SdoSys.KEY_FILEDESC, b64);
    list.add(entry);

    entry = new SimpleEntry<>(SdoSys.KEY_WRITE, new FileBase64Sequence(getLocal()));
    list.add(entry);

    return list;
  }

  private Path getLocal() {
    return local;
  }

  private Path getRemote() {
    return remote;
  }

  static class Builder {

    private Path local = null;
    private Path remote = null;

    /**
     * Construct a new builder.
     */
    public FileDownloadSource build() {
      if (null == local || null == remote) {
        throw new IllegalStateException();
      }
      return new FileDownloadSource(local, remote);
    }

    public Builder local(Path value) {
      local = value;
      return this;
    }

    public Builder remote(Path value) {
      remote = value;
      return this;
    }
  }
}
