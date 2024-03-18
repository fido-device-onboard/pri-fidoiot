// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.math.NumberUtils;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.fidoalliance.fdo.protocol.message.RendezvousDirective;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.fidoalliance.fdo.protocol.message.RendezvousInstruction;
import org.fidoalliance.fdo.protocol.message.RendezvousProtocol;
import org.fidoalliance.fdo.protocol.message.RendezvousVariable;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.fidoalliance.fdo.protocol.message.To2AddressEntry;
import org.fidoalliance.fdo.protocol.message.TransportProtocol;

public class HttpUtils {

  public static final String HTTP_AUTHORIZATION = "Authorization";
  public static final String HTTP_APPLICATION_CBOR = "application/cbor";
  public static final String HTTP_PLAIN_TEXT = "text/plain";
  public static final String HTTP_MESSAGE_TYPE = "Message-Type";
  public static final String HTTP_CONTENT_TYPE = "Content-Type";
  public static final String HTTP_BEARER = "Bearer ";
  public static final String HTTP_SCHEME = "http";
  public static final String HTTPS_SCHEME = "https";


  public static final String FDO_COMPONENT = "fdo";
  public static final String MSG_COMPONENT = "msg";

  private static final int URI_PART_FDO = 0;
  private static final int URI_PART_PROTOCOL = 1;
  private static final int URI_PART_MSG = 2;
  private static final int URI_PART_MSG_ID = 3;

  /**
   * Builds a dispatch message from an uri.
   *
   * @param uri A fdo spec URI.
   * @return A dispatch message representing the uri.
   * @throws IOException An error occurred.
   */
  public static DispatchMessage getMessageFromUri(String uri) throws IOException {

    //URI is in form /fdo/<protocolver>/msg/<msgType>
    DispatchMessage message = new DispatchMessage();

    File segFile = new File(uri);
    if (NumberUtils.isCreatable(segFile.getName())) {
      message.setMsgType(MsgType.fromNumber(Integer.parseInt(segFile.getName())));
    } else {
      throw new InvalidPathException(uri, "msgType not a number");
    }
    segFile = segFile.getParentFile();
    if (segFile == null || !segFile.getName().equals(HttpUtils.MSG_COMPONENT)) {
      throw new InvalidPathException(uri, "msg expected");
    }

    segFile = segFile.getParentFile();

    if (segFile == null || !NumberUtils.isCreatable(segFile.getName())) {
      throw new InvalidPathException(uri, "protocol version not a number");
    } else {
      message.setProtocolVersion(ProtocolVersion.fromString(segFile.getName()));
    }

    segFile = segFile.getParentFile();
    if (segFile == null || !segFile.getName().equals(HttpUtils.FDO_COMPONENT)) {
      throw new InvalidPathException(uri, "fdo expected");
    }

    return message;
  }

  private static boolean containsRvVariable(RendezvousDirective directive, RendezvousVariable val) {

    for (RendezvousInstruction instruction : directive) {
      RendezvousVariable variable = instruction.getVariable();
      if (variable == val) {
        return true;
      }
    }
    return  false;
  }

  /**
   * Gets a list of URL Strings.
   *
   * @param info     An instance of RendezvousInfo.
   * @param isDevice True if device is asking and false if owner.
   * @return A List or URL strings.
   * @throws IOException An error occurred.
   */
  public static List<HttpInstruction> getInstructions(RendezvousInfo info, boolean isDevice)
      throws IOException {

    List<HttpInstruction> list = new ArrayList<>();
    for (RendezvousDirective directive : info) {
      String dns = null;
      String ipAddress = null;
      Integer devPort = Integer.valueOf(80);
      Integer ownerPort = Integer.valueOf(443);
      List<String> devSchemes = new ArrayList<>();

      long delaySec = 0;
      boolean ownerOnly = false;
      boolean devOnly = false;
      boolean bypass = false;

      for (RendezvousInstruction instruction : directive) {

        RendezvousVariable variable = instruction.getVariable();

        switch (variable) {
          case DNS:
            dns = Mapper.INSTANCE.readValue(instruction.getValue(), String.class);
            break;
          case IP_ADDRESS:
            try {
              byte[] ipData = Mapper.INSTANCE.readValue(instruction.getValue(), byte[].class);
              ipAddress = InetAddress.getByAddress(
                  ipData
              ).toString();
              int pos = ipAddress.lastIndexOf('/');
              if (pos >= 0) {
                ipAddress = ipAddress.substring(pos + 1);
              }
              //check for IPv6 address string
              pos = ipAddress.indexOf((':'));
              if (pos > 0) {
                // add brackets for IP address
                ipAddress = "[" + ipAddress + "]";
              }
            } catch (UnknownHostException e) {
              throw new InvalidIpAddressException(e);
            }
            break;
          case DEV_PORT:
            devPort = Mapper.INSTANCE.readValue(instruction.getValue(), Integer.class);
            break;
          case OWNER_PORT:
            ownerPort = Mapper.INSTANCE.readValue(instruction.getValue(), Integer.class);
            break;
          case PROTOCOL: {
            RendezvousProtocol rvp = Mapper.INSTANCE.readValue(instruction.getValue(),
                RendezvousProtocol.class);
            if (rvp.equals(RendezvousProtocol.PROT_HTTPS)) {
              devSchemes.add(HTTPS_SCHEME);
            } else if (rvp.equals(RendezvousProtocol.PROT_HTTP)) {
              devSchemes.add(HTTP_SCHEME);
            } else if (rvp.equals(RendezvousProtocol.PROT_REST)) {
              devSchemes.add(HTTPS_SCHEME);
              devSchemes.add(HTTP_SCHEME);
            }
          }
          break;
          case OWNER_ONLY:
            ownerOnly = true;
            break;
          case DEV_ONLY:
            devOnly = true;
            break;
          case DELAYSEC:
            delaySec = Mapper.INSTANCE.readValue(instruction.getValue(), Long.class);
            break;
          case BYPASS:
            bypass = true;
            break;
          default:
            break;
        }
      }

      if (devSchemes.isEmpty()) {
        devSchemes.add(HTTPS_SCHEME);
      }
      //done getting both
      if (!isDevice && devOnly) {
        continue;
      }
      if (isDevice && ownerOnly) {
        continue;
      }

      if (dns == null && ipAddress == null) {
        continue;
      }

      List<String> schemes = new ArrayList<>();
      Integer port = null;
      if (isDevice) {
        schemes = devSchemes;
        port = devPort;
      } else {
        if (ownerPort.equals((devPort)) && devSchemes.contains(HTTP_SCHEME)
            && devSchemes.size() == 1
            && !ownerOnly) {
          schemes = new ArrayList<>();
          schemes.add(HTTP_SCHEME);
        } else {
          schemes = Config.getWorker(OwnerSchemesSupplier.class).get();
        }

        port = ownerPort;
      }
      for (String scheme : schemes) {
        int assignedPort = 0;
        if (port == null) {
          if (scheme.equals(HTTP_SCHEME)) {
            assignedPort = Integer.valueOf(80);
          } else if (scheme.equals(HTTPS_SCHEME)) {
            assignedPort = Integer.valueOf(443);
          }
        } else {
          assignedPort = port;
        }

        if (dns != null) {
          HttpInstruction httpInst = new HttpInstruction();
          if (!containsRvVariable(directive, RendezvousVariable.IP_ADDRESS)) {
            httpInst.setDelay(delaySec);
          } else {
            httpInst.setDelay(0);
          }
          httpInst.setAddress(scheme + "://" + dns + ":" + assignedPort);
          httpInst.setRendezvousBypass(bypass);
          list.add(httpInst);
        }
        if (ipAddress != null) {
          HttpInstruction httpInst = new HttpInstruction();
          httpInst.setDelay(delaySec);
          httpInst.setAddress(scheme + "://" + ipAddress + ":" + assignedPort);
          httpInst.setRendezvousBypass(bypass);
          list.add(httpInst);
        }
      }

    }

    return list;
  }

  /**
   * Get Http Instructions from To2Address Entries.
   *
   * @param entries An Instance of To2 Address Entries.
   * @return A list of Http Instructions.
   * @throws IOException An Error occurred.
   */
  public static List<HttpInstruction> getInstructions(To2AddressEntries entries)
      throws IOException {
    List<HttpInstruction> list = new ArrayList<>();
    String scheme = "https";

    for (To2AddressEntry entry : entries) {

      if (entry.getProtocol() == TransportProtocol.PROT_HTTP) {
        scheme = HTTP_SCHEME;
      } else if (entry.getProtocol() == TransportProtocol.PROT_HTTPS) {
        scheme = HTTPS_SCHEME;
      } else {
        continue;
      }

      if (entry.getDnsAddress() != null) {
        HttpInstruction httpInst = new HttpInstruction();
        httpInst.setDelay(0);
        httpInst.setAddress(scheme + "://" + entry.getDnsAddress() + ":" + entry.getPort());
        httpInst.setRendezvousBypass(false);
        list.add(httpInst);
      }

      if (entry.getIpAddress() != null) {
        try {
          InetAddress address = InetAddress.getByAddress(entry.getIpAddress());
          String ipAddress = address.toString();
          int pos = ipAddress.lastIndexOf('/');
          if (pos >= 0) {
            ipAddress = ipAddress.substring(pos + 1);
          }
          HttpInstruction httpInst = new HttpInstruction();
          httpInst.setDelay(0);
          httpInst.setAddress(scheme + "://" + ipAddress + ":" + entry.getPort());
          httpInst.setRendezvousBypass(false);
          list.add(httpInst);
        } catch (UnknownHostException e) {
          throw new InvalidIpAddressException(e);
        }

      }

    }
    if (list.isEmpty()) {
      throw new RuntimeException("Invalid T02_RVBLOB.");
    }
    return list;
  }

  /**
   * See if two lists of HTTP instructions have the same host in common.
   *
   * @param h1 List of Rv Instructions.
   * @param h2 List of To0d HTTP instructions.
   * @return true if the owner and RV hosts are the same.
   */
  public static boolean containsAddress(List<HttpInstruction> h1, List<HttpInstruction> h2) {
    List<String> list1 = new ArrayList<>();
    List<String> list2 = new ArrayList<>();
    for (HttpInstruction instruction : h1) {
      URI uri = URI.create(instruction.getAddress());
      list1.add(uri.getHost());
    }

    for (HttpInstruction instruction : h2) {
      URI uri = URI.create(instruction.getAddress());
      list2.add(uri.getHost());
    }

    Set<String> result = list1.stream()
        .distinct()
        .filter(list2::contains)
        .collect(Collectors.toSet());

    return result.size() > 0;

  }
}
