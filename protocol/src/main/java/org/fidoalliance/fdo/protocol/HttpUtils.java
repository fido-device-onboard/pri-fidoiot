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
import javax.persistence.criteria.CriteriaBuilder.In;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.protocol.HTTP;
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
import org.hibernate.type.AnyType;

public class HttpUtils {

  public static final String HTTP_AUTHORIZATION = "Authorization";
  public static final String HTTP_APPLICATION_CBOR = "application/cbor";
  public static final String HTTP_PLAIN_TEXT = "text/plain";
  public static final String HTTP_MESSAGE_TYPE = "Message-Type";
  public static final String HTTP_CONTENT_TYPE = "Content-Type";
  public static final String HTTP_BEARER = "Bearer";
  public static final String HTTP_SCHEME = "http";
  public static final String HTTPS_SCHEME = "https";


  public static final String FDO_COMPONENT = "fdo";
  public static final String MSG_COMPONENT = "msg";

  private static final int URI_PART_FDO = 0;
  private static final int URI_PART_PROTOCOL = 1;
  private static final int URI_PART_MSG = 2;
  private static final int URI_PART_MSG_ID = 3;

  public static DispatchMessage getMessageFromURI(String uri) throws IOException {

    //URI is in form /fdo/<protocolver>/msg/<msgType>
    DispatchMessage message = new DispatchMessage();

    File segFile = new File(uri);
    if (NumberUtils.isCreatable(segFile.getName())) {
      message.setMsgType(MsgType.fromNumber(Integer.parseInt(segFile.getName())));
    } else {
      throw new InvalidPathException(uri, "msgType not a number");
    }
    segFile = segFile.getParentFile();
    if (!segFile.getName().equals(HttpUtils.MSG_COMPONENT)) {
      throw new InvalidPathException(uri, "msg expected");
    }

    segFile = segFile.getParentFile();
    if (NumberUtils.isCreatable(segFile.getName())) {
      message.setProtocolVersion(ProtocolVersion.fromString(segFile.getName()));
    } else {
      throw new InvalidPathException(uri, "protocol version not a number");
    }

    segFile = segFile.getParentFile();
    if (!segFile.getName().equals(HttpUtils.FDO_COMPONENT)) {
      throw new InvalidPathException(uri, "fdo expected");
    }

    return message;
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
      List<String> devSchemes =  new ArrayList<>();
      List<String> ownerSchemes = new ArrayList<>();
      ownerSchemes.add(HTTPS_SCHEME);
      ownerSchemes.add(HTTP_SCHEME);



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
            delaySec = Mapper.INSTANCE.readValue(instruction.getValue(),Long.class);
            break;
          case BYPASS:
            bypass = true;
            break;
          default:
            break;
        }
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
      if (dns == null && ipAddress == null) {
        continue;
      }

      List<String> schemes = new ArrayList<>();
      Integer port = null;
      if (isDevice) {
        schemes = devSchemes;
        port = devPort;
      } else {
        schemes = ownerSchemes;
        port = ownerPort;
      }
      for (String scheme : schemes) {
        int assignedPort = 0;
        if (port == null) {
          if (scheme.equals("http")) {
            assignedPort = Integer.valueOf(80);
          } else if (scheme.equals("https")) {
            assignedPort = Integer.valueOf(443);
          }
        } else {
          assignedPort = port;
        }

        if (dns != null) {
          HttpInstruction httpInst = new HttpInstruction();
          httpInst.setDelay(delaySec);
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
    List<String> schemes = new ArrayList<>();

    for (To2AddressEntry entry : entries) {

      if (entry.getProtocol() == TransportProtocol.PROT_HTTP) {
        schemes.add("http");
      } else if (entry.getProtocol() == TransportProtocol.PROT_HTTPS) {
        schemes.add("https");
      } else {
        continue;
      }

      for (String scheme : schemes) {
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

    }
    return list;
  }

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
