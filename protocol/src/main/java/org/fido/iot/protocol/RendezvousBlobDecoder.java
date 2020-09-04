// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes a Rendezvous 'Blob' String.
 */
public class RendezvousBlobDecoder {

  private static byte[] getIpAddress(String value) {

    try {
      return InetAddress.getByName(value).getAddress();
    } catch (UnknownHostException e) {
      throw new DispatchException(e);
    }
  }

  /**
   * Decodes a Rendezvous 'Blob' String.
   *
   * @param blobString A Rendezvous 'Blob' encoded as a space separated string.
   * @return A Composite representing Rendezvous 'Blob'.
   */
  public static Composite decode(String blobString) {
    Composite entries = Composite.newArray();
    String[] daArray = blobString.split("\\s+");

    for (String directive : daArray) {
      Composite blob = Composite.newArray();
      URI uri = URI.create(directive);

      String[] queryParams = uri.getQuery().split("&");
      for (String param : queryParams) {
        String[] pair = param.split("=");

        if (pair[0].compareToIgnoreCase("ipaddress") == 0) {
          blob.set(Const.BLOB_IP_ADDRESS,
              getIpAddress(pair[1]));
        }
      }
      if (blob.size() == 0) {
        throw new MessageBodyException(new InvalidParameterException());
      }

      blob.set(Const.BLOB_DNS, uri.getHost());
      blob.set(Const.BLOB_PORT, uri.getPort());

      //set the protocol
      String scheme = uri.getScheme();
      switch (scheme) {
        case "tcp":
          blob.set(Const.BLOB_PROTOCOL, Const.BLOB_PROT_TCP);
          break;
        case "tls":
          blob.set(Const.BLOB_PROTOCOL, Const.BLOB_PROT_TLS);
          break;
        case "http":
          blob.set(Const.BLOB_PROTOCOL, Const.BLOB_PROT_HTTP);
          break;
        case "coap":
          blob.set(Const.BLOB_PROTOCOL, Const.BLOB_PROT_COAP);
          break;
        case "https":
          blob.set(Const.BLOB_PROTOCOL, Const.BLOB_PROT_HTTPS);
          break;
        case "coaps":
          blob.set(Const.BLOB_PROTOCOL, Const.BLOB_PROT_COAPS);
          break;
        default:
          throw new DispatchException(new InvalidParameterException());
      }

      entries.set(entries.size(), blob);

    }
    return entries;
  }

  /**
   * Gets the HTTP Rendezvous directives encoded as a URI string.
   *
   * @param singedBlob A cose singed Rendezvous 'Blob'.
   * @return A list of HTTP uri's.
   */
  public static List<String> getHttpDirectives(Composite singedBlob) {
    List<String> list = new ArrayList<String>();

    final byte[] payload = singedBlob.getAsBytes(Const.COSE_SIGN1_PAYLOAD);
    Composite to1d = Composite.fromObject(payload);
    Composite directives = to1d.getAsComposite(Const.TO1D_RV);
    for (int i = 0; i < directives.size(); i++) {
      Composite directive = directives.getAsComposite(i);
      byte[] ipBytes = directive.getAsBytes(Const.BLOB_IP_ADDRESS);
      String dns = directive.getAsString(Const.BLOB_DNS);
      int port = directive.getAsNumber(Const.BLOB_PORT).intValue();
      int protocol = directive.getAsNumber(Const.BLOB_PROTOCOL).intValue();
      if (protocol == Const.BLOB_PROT_HTTP || protocol == Const.BLOB_PROT_HTTPS) {
        String protocolString = "http://";
        if (protocol == Const.BLOB_PROT_HTTPS) {
          protocolString = "https://";
        }

        InetAddress netAdress = null;
        try {
          netAdress = InetAddress.getByAddress(ipBytes);
        } catch (UnknownHostException e) {
          throw new InvalidIpAddressException(e);
        }
        String ipAddrString = netAdress.getHostAddress();

        list.add(protocolString + dns + ":" + Integer.toString(port));
        list.add(protocolString + ipAddrString + ":" + Integer.toString(port));

      }
    }

    return list;
  }

}
