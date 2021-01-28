// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.serviceinfo.SdoDev;
import org.fido.iot.serviceinfo.SdoSys;
import org.fido.iot.serviceinfo.ServiceInfoEntry;
import org.fido.iot.serviceinfo.ServiceInfoModule;
import org.fido.iot.serviceinfo.ServiceInfoSequence;

public class DeviceServiceInfoModule implements ServiceInfoModule {

  // Keys for localized log format strings in our resource bundle
  private static final String LOG_ERR_PATH_NULL =
      DeviceServiceInfoModule.class.getName() + ".LOG_ERR_PATH_NULL";
  private static final String LOG_INFO_FILE_CREATED =
      DeviceServiceInfoModule.class.getName() + ".LOG_INFO_FILE_CREATED";
  private static final Character EXEC_TERMINATOR = '\0';
  private StringBuilder execBuilder = new StringBuilder();
  private ProcessBuilder.Redirect execOutputRedirect = ProcessBuilder.Redirect.PIPE;
  private Duration execTimeout = Duration.ofHours(2);
  private Predicate<Integer> exitValueTest = val -> (0 == val);
  private StringBuilder filenameBuilder = new StringBuilder();
  private Path path = null;

  private Duration getExecTimeout() {
    return execTimeout;
  }

  private Predicate<Integer> getExitValueTest() {
    return exitValueTest;
  }

  @Override
  public List<ServiceInfoEntry> getServiceInfo(UUID uuid) {
    List<ServiceInfoEntry> serviceInfoEntries = new LinkedList<>();

    ServiceInfoSequence valueSequence = new DeviceServiceInfoSequence("1", true, 1);
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_ACTIVE, valueSequence));

    valueSequence =
        new DeviceServiceInfoSequence(
            "2", System.getProperty("os.name"), System.getProperty("os.name").length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_OS, valueSequence));

    valueSequence =
        new DeviceServiceInfoSequence(
            "3", System.getProperty("os.arch"), System.getProperty("os.arch").length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_ARCH, valueSequence));

    valueSequence =
        new DeviceServiceInfoSequence(
            "4", System.getProperty("os.version"), System.getProperty("os.version").length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_VERSION, valueSequence));

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SDO reference device, using JRE ");
    stringBuilder.append(System.getProperty("java.version"));
    stringBuilder.append(" (");
    stringBuilder.append(System.getProperty("java.vendor"));
    stringBuilder.append(")");
    valueSequence =
        new DeviceServiceInfoSequence("5", stringBuilder.toString(), stringBuilder.length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_DEVICE, valueSequence));

    valueSequence = new DeviceServiceInfoSequence("6", "", 0);
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_SN, valueSequence));

    valueSequence = new DeviceServiceInfoSequence("7", File.separator, File.separator.length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_PATHSEP, valueSequence));

    valueSequence =
        new DeviceServiceInfoSequence(
            "8",
            Character.toString(File.pathSeparatorChar),
            Character.toString(File.pathSeparatorChar).length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_SEP, valueSequence));

    valueSequence =
        new DeviceServiceInfoSequence(
            "9",
            System.getProperty("line.separator"),
            System.getProperty("line.separator").length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_NL, valueSequence));

    valueSequence =
        new DeviceServiceInfoSequence(
            "10",
            System.getProperty("java.io.tmpdir"),
            System.getProperty("java.io.tmpdir").length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_TMP, valueSequence));

    valueSequence =
        new DeviceServiceInfoSequence(
            "11",
            System.getProperty("java.io.tmpdir"),
            System.getProperty("java.io.tmpdir").length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_DIR, valueSequence));

    valueSequence = new DeviceServiceInfoSequence("12", "bin:java", "bin:java".length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_PROGENV, valueSequence));

    valueSequence =
        new DeviceServiceInfoSequence(
            "13", System.getProperty("os.arch"), System.getProperty("os.arch").length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_BIN, valueSequence));

    valueSequence = new DeviceServiceInfoSequence("14", "", 0);
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_MUDURL, valueSequence));

    valueSequence = new DeviceServiceInfoSequence("15", "3", "3".length());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_NUMMODULES, valueSequence));

    Composite value =
        Composite.newArray()
            .set(Const.FIRST_KEY, 3)
            .set(Const.SECOND_KEY, 3)
            .set(Const.THIRD_KEY, "modname")
            .set(Const.FOURTH_KEY, "binaryfile")
            .set(Const.FIFTH_KEY, "wget");
    valueSequence = new DeviceServiceInfoSequence("16", value, value.size());
    valueSequence.initSequence();
    serviceInfoEntries.add(new ServiceInfoEntry(SdoDev.KEY_MODULES, valueSequence));

    return serviceInfoEntries;
  }

  @Override
  public void putServiceInfo(UUID uuid, ServiceInfoEntry entry) {

    String key = entry.getKey();

    switch (key) {
      case (SdoSys.NAME + ":" + SdoSys.KEY_FILEDESC):
        acceptFileDesc((byte[]) entry.getValue().getContent());
        break;
      case (SdoSys.NAME + ":" + SdoSys.KEY_WRITE):
        try {
          acceptWrite((byte[]) entry.getValue().getContent());

        } catch (IOException e) {
          System.out.println(e.getMessage());
        }
        break;
      case (SdoSys.NAME + ":" + SdoSys.KEY_EXEC):
        try {
          acceptExec((byte[]) entry.getValue().getContent());

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        break;
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
          boolean processDone = process.waitFor(getExecTimeout().toMillis(), TimeUnit.MILLISECONDS);
          if (processDone) {
            if (!getExitValueTest().test(process.exitValue())) {
              throw new RuntimeException(
                  "predicate failed: "
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

  private ProcessBuilder.Redirect getExecOutputRedirect() {
    return execOutputRedirect;
  }

  private void acceptExec(byte[] encodedValue)
      throws InterruptedException, IOException, TimeoutException {

    // Because the instruction could be fragmented across packets, accumulate it
    // until an instruction terminator is found.

    Character[] history = {null, null};
    int historyIx = 0;

    StringBuilder execBuilder = getExecBuilder();
    if (execBuilder.length() > 0) {
      history[historyIx++] = execBuilder.charAt(execBuilder.length() - 1);
    }

    ByteBuffer execBytes = ByteBuffer.wrap(encodedValue);
    while (execBytes.hasRemaining()) {

      byte[] execByte = new byte[] { execBytes.get() };
      char c = SdoSys.CHARSET.decode(ByteBuffer.wrap(execByte)).get();
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

  private void acceptFileDesc(byte[] encodedValue) {
    getFilenameBuilder().append(SdoSys.CHARSET.decode(ByteBuffer.wrap(encodedValue)));
  }

  private void acceptWrite(byte[] encodedValue) throws IOException {

    // The first WRITE instruction after a FILEDESC instruction signals that
    // FILEDESC is complete and can now be latched.
    if (getFilenameBuilder().length() > 0) {
      createFile(Paths.get(getFilenameBuilder().toString()));
      setFilenameBuilder(new StringBuilder()); // reset builder for next FILEDESC
    }

    Path path = getPath();

    if (null == path) {
      throw new IllegalStateException(LOG_ERR_PATH_NULL);
    }

    try (FileChannel channel = FileChannel.open(getPath(), StandardOpenOption.APPEND)) {
      channel.write(ByteBuffer.wrap(encodedValue));
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
        System.out.println(LOG_INFO_FILE_CREATED);
      }

    } else {

      try (FileChannel channel = FileChannel.open(path, openOptions)) {
        // opening the channel is enough to create the file
      }
    }

    setPath(path);
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

  private Path getPath() {
    return path;
  }

  private Path setPath(Path value) {
    path = value;
    return value;
  }
}
