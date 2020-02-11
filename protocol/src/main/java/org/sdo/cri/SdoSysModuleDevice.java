// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SDO ServiceInfo System Module, device-side.
 */
public final class SdoSysModuleDevice implements ServiceInfoSource, ServiceInfoSink {

  private static final int BASE64_ENCODED_BLOCKSIZE = 4;

  // Keys for localized log format strings in our resource bundle
  //
  private static final String LOG_ERR_PATH_NULL =
      SdoSysModuleDevice.class.getName() + ".LOG_ERR_PATH_NULL";
  private static final String LOG_INFO_FILE_CREATED =
      SdoSysModuleDevice.class.getName() + ".LOG_INFO_FILE_CREATED";
  private static final ResourceBundle bundle_ =
      ResourceBundle.getBundle(SdoSysModuleDevice.class.getName(), Locale.getDefault());
  private static final Character EXEC_TERMINATOR = '\0';
  private final CharBuffer b64Buf = CharBuffer.allocate(BASE64_ENCODED_BLOCKSIZE);
  private StringBuilder execBuilder = new StringBuilder();
  private Redirect execOutputRedirect = Redirect.PIPE;
  private Duration execTimeout = Duration.ofSeconds(60);
  private Predicate<Integer> exitValueTest = val -> (0 == val);
  private StringBuilder filenameBuilder = new StringBuilder();
  private Path path = null;

  private Duration getExecTimeout() {
    return execTimeout;
  }

  /**
   * Set the sdo_sys:exec timeout.
   *
   * @param value The new timeout.  execs which take longer than this are terminated, resulting in a
   *              TimeoutException.
   */
  public void setExecTimeout(Duration value) {
    if (null == value) {
      throw new IllegalArgumentException();
    }
    execTimeout = value;
  }

  private Predicate<Integer> getExitValueTest() {
    return exitValueTest;
  }

  /**
   * Set the sdo_sys:exec exit-value test.
   *
   * <p>Set the Predicate used to evaluate subprocess exit values for pass/fail.
   * The default Predicate passes iff exit value is 0.
   *
   * @param value The new timeout.  execs which take longer than this are terminated, resulting in a
   *              TimeoutException.
   */
  public void setExitValueTest(Predicate<Integer> value) {
    if (null == value) {
      throw new IllegalArgumentException();
    }
    exitValueTest = value;
  }

  @Override
  public List<Entry<CharSequence, CharSequence>> getServiceInfo() {
    return Collections.singletonList(new SimpleEntry<>(SdoSys.KEY_ACTIVE, "1"));
  }

  @Override
  public void putServiceInfo(Entry<CharSequence, CharSequence> entry) {

    if (null == entry || null == entry.getKey() || null == entry.getValue()) {
      throw new IllegalArgumentException(); // null entries are nonsensical
    }

    final String key = entry.getKey().toString();

    switch (key) {

      case SdoSys.KEY_FILEDESC: {
        acceptFileDesc(CharBuffer.wrap(entry.getValue()));
        break;
      }

      case SdoSys.KEY_WRITE: {

        try {
          acceptWrite(CharBuffer.wrap(entry.getValue()));

        } catch (IOException e) {
          getLogger().error(e.getMessage());
        }
        break;
      }

      case SdoSys.KEY_EXEC: {

        try {
          acceptExec(CharBuffer.wrap(entry.getValue()));

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        break;
      }

      default:
        // not one of our keys, just ignore it
    }
  }

  void exec(String command) throws InterruptedException, IOException, TimeoutException {

    int endIx = command.lastIndexOf(EXEC_TERMINATOR + EXEC_TERMINATOR);
    if (endIx > 0) { // terminator found and has something before it
      command = command.substring(0, endIx);
      String[] args = command.split(EXEC_TERMINATOR.toString());

      try {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);
        builder.redirectOutput(getExecOutputRedirect());
        Process process = builder.start();
        try {
          boolean processDone =
              process.waitFor(getExecTimeout().toMillis(), TimeUnit.MILLISECONDS);
          if (processDone) {
            if (!getExitValueTest().test(process.exitValue())) {
              throw new RuntimeException("predicate failed: "
                  + Arrays.toString(args)
                  + " returned "
                  + process.exitValue());
            }
          } else { // timeout
            throw new TimeoutException(Arrays.toString(args));
          }

        } finally {

          if (process.isAlive()) {
            process.destroyForcibly();
          }
        }

      } catch (IndexOutOfBoundsException | NullPointerException e) {
        throw new IllegalArgumentException(); // command list was empty (ill-formed)
      }

    } else {
      throw new IllegalArgumentException(); // missing terminator
    }
  }

  private Redirect getExecOutputRedirect() {
    return execOutputRedirect;
  }

  public void setExecOutputRedirect(Redirect value) {
    execOutputRedirect = Objects.requireNonNull(value);
  }

  private void acceptExec(CharBuffer encodedValue)
      throws InterruptedException, IOException, TimeoutException {

    // Because the instruction could be fragmented across packets, accumulate it
    // until an instruction terminator is found.

    Character[] history = {null, null};
    int historyIx = 0;

    StringBuilder execBuilder = getExecBuilder();
    if (execBuilder.length() > 0) {
      history[historyIx++] = execBuilder.charAt(execBuilder.length() - 1);
    }

    for (ByteBuffer rawBytes = decodeOneBlock(encodedValue);
        rawBytes.hasRemaining();
        rawBytes = decodeOneBlock(encodedValue)) { // for each full b64 block...

      CharBuffer rawChars = SdoSys.CHARSET.decode(rawBytes); // SDO_SYS uses its own charset

      while (rawChars.hasRemaining()) {

        char c = rawChars.get();
        execBuilder.append(c);

        // Does this character complete an instruction?
        history[historyIx] = c;
        historyIx = (++historyIx) % history.length;
        boolean isTerminated = true;

        for (Character h : history) { // are both history characters TERMINATOR?
          if (!Objects.equals(h, EXEC_TERMINATOR)) {
            isTerminated = false;
          }
        }

        if (isTerminated) {
          exec(execBuilder.toString());
          setExecBuilder(new StringBuilder());
        }
      }
    }
  }

  // Per the module specification:
  //
  // A zero length file is expected to exist on the local file system
  // after this command is received.
  //
  // If the described file already exists it is truncated to zero length,
  // otherwise a zero length file is created.
  //
  // The file needs to be created its [sic] permissions are set to read/write for
  // the user account the module is running under.
  //
  // If a path is not included as a part of the file name,
  // the current working directory of the module is assumed.
  //
  private void acceptFileDesc(CharBuffer encodedValue) {

    // Because the file name could be fragmented across packets, do not
    // attempt to create it right away.  When the first WRITE key arrives,
    // that's proof that the FILEDESC has ended and we can create it then.
    // Once we open the file, we can clear the buffer for the next descriptor.

    for (ByteBuffer rawBytes = decodeOneBlock(encodedValue);
        rawBytes.hasRemaining();
        rawBytes = decodeOneBlock(encodedValue)) { // for each full b64 block...

      CharBuffer rawChars = SdoSys.CHARSET.decode(rawBytes); // SDO_SYS uses its own charset
      getFilenameBuilder().append(rawChars);
    }
  }

  // Per the module specification:
  //
  // Write an array of bytes to the end of the file described by the last
  // sdo_sys.filedesc command.
  //
  // If this message is sent without being preceded by sdo_sys.filedesc then an error
  // will be thrown and TO2 will not complete.
  private void acceptWrite(CharBuffer encodedValue) throws IOException {

    // The first WRITE instruction after a FILEDESC instruction signals that
    // FILEDESC is complete and can now be latched.
    if (getFilenameBuilder().length() > 0) {
      createFile(Paths.get(getFilenameBuilder().toString()));
      setFilenameBuilder(new StringBuilder()); // reset builder for next FILEDESC
    }

    Path path = getPath();

    if (null == path) {
      String logMessage = getResourceBundle().getString(LOG_ERR_PATH_NULL);
      getLogger().error(logMessage);
      throw new IllegalStateException(logMessage);
    }

    try (FileChannel channel = FileChannel.open(getPath(), StandardOpenOption.APPEND)) {

      for (ByteBuffer rawBytes = decodeOneBlock(encodedValue);
          rawBytes.hasRemaining();
          rawBytes = decodeOneBlock(encodedValue)) { // for each full b64 block...

        channel.write(rawBytes);
      }
    }
  }

  private void createFile(Path path) throws IOException {

    Set<OpenOption> openOptions = new HashSet<>();
    openOptions.add(StandardOpenOption.CREATE);
    openOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
    openOptions.add(StandardOpenOption.WRITE);

    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {

      Set<PosixFilePermission> filePermissions = new HashSet<>();
      filePermissions.add(PosixFilePermission.OWNER_READ);
      filePermissions.add(PosixFilePermission.OWNER_WRITE);
      FileAttribute<?> fileAttribute = PosixFilePermissions.asFileAttribute(filePermissions);

      try (FileChannel channel = FileChannel.open(path, openOptions, fileAttribute)) {

        String format = getResourceBundle().getString(LOG_INFO_FILE_CREATED);
        String logMessage = MessageFormat.format(format, path,
            PosixFilePermissions.toString(filePermissions));
        getLogger().info(logMessage);
      }

    } else {

      try (FileChannel channel = FileChannel.open(path, openOptions)) {
        // opening the channel is enough to create the file
      }
    }

    setPath(path);
  }

  private ByteBuffer decodeOneBlock(CharBuffer input) {
    CharBuffer blockChars = getB64Buf();
    Base64.Decoder b64dec = Base64.getDecoder();

    if (input.remaining() >= blockChars.remaining()) { // if we can complete a block

      while (blockChars.hasRemaining()) { // complete the current block
        blockChars.put(input.get());
      }

      blockChars.flip(); // done writing the block, prepare to read
      ByteBuffer blockBytes = ISO_8859_1.encode(blockChars); // base64 uses ISO_8859_1
      blockChars.flip(); // prepare to write the next block

      return b64dec.decode(blockBytes);
    }

    while (input.remaining() > 0) { // store the trailing short block
      blockChars.put(input.get());
    }

    return ByteBuffer.allocate(0); // short block, no return
  }

  private CharBuffer getB64Buf() {
    return b64Buf;
  }

  private StringBuilder getExecBuilder() {
    return execBuilder;
  }

  private void setExecBuilder(StringBuilder execBuilder) {
    this.execBuilder = execBuilder;
  }

  private StringBuilder getFilenameBuilder() {
    return filenameBuilder;
  }

  private void setFilenameBuilder(StringBuilder filenameBuilder) {
    this.filenameBuilder = filenameBuilder;
  }

  private Logger getLogger() {
    return LoggerFactory.getLogger(getClass());
  }

  private Path getPath() {
    return path;
  }

  private ResourceBundle getResourceBundle() {
    return bundle_;
  }

  private Path setPath(Path value) {
    path = value;
    return value;
  }
}
