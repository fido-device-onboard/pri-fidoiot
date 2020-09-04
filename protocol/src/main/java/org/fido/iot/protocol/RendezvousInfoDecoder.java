// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes RendezvousInfo strings.
 */
public class RendezvousInfoDecoder {

  private static boolean getBoolean(String value) {
    if (value.compareToIgnoreCase("true") == 0) {
      return true;
    } else if (value.compareToIgnoreCase("false") == 0) {
      return false;
    }
    throw new DispatchException(new InvalidParameterException(value));
  }

  private static byte[] getIpAddress(String value) {

    try {
      return InetAddress.getByName(value).getAddress();
    } catch (UnknownHostException e) {
      throw new DispatchException(e);
    }
  }

  private static Object getDirective(String[] param) {

    switch (param[0].toLowerCase()) {
      case "devonly":
        return new Object[]{Const.RV_DEV_ONLY};
      case "owneronly":
        return new Object[]{Const.RV_OWNER_ONLY};
      case "ipaddress":
        return new Object[]{Const.RV_IP_ADDRESS, getIpAddress(param[1])};
      case "ownerport":
        return new Object[]{Const.RV_OWNER_PORT, Integer.parseInt(param[1])};
      case "delaysec":
        return new Object[]{Const.RV_DELAY_SEC, Integer.parseInt(param[1])};
      case "svcerthash":
        return new Object[]{Const.RV_SV_CERT_HASH, Composite.fromObject(param[1])};//decoce hash
      case "clcerthash":
        return new Object[]{Const.RV_CLT_CERT_HASH, Composite.fromObject(param[1])}; //decode hasdh
      case "userinput":
        return new Object[]{Const.RV_USER_INPUT, getBoolean(param[1])};
      case "medium":
        return Integer.parseInt(param[1]);
      case "wifissid":
        return new Object[]{Const.RV_WIFI_SSID,
            URLDecoder.decode(param[1], StandardCharsets.US_ASCII)};
      case "wifipw":
        return new Object[]{Const.RV_WIFI_PW,
            URLDecoder.decode(param[1], StandardCharsets.US_ASCII)};
      default:
        throw new DispatchException(new InvalidParameterException(param[0]));
    }
  }

  /**
   * Decodes a Rendezvous instruction string.
   *
   * @param directives A space separated string of encoded directives.
   * @return A Composite representing RendezvousInfo.
   */
  public static Composite decode(String directives) {
    String[] daArray = directives.split("\\s+");
    Composite result = Composite.newArray();

    for (String directive : daArray) {
      Composite rd = Composite.newArray();
      URI uri = URI.create(directive);

      rd.set(rd.size(), new Object[]{Const.RV_DNS, uri.getHost()});
      rd.set(rd.size(), new Object[]{Const.RV_DEV_PORT, uri.getPort()});

      //set the protocol
      String scheme = uri.getScheme();
      switch (scheme) {
        case "http":
          rd.set(rd.size(), new Object[]{Const.RV_PROTOCOL, Const.RV_PROT_HTTP});
          break;
        case "https":
          rd.set(rd.size(), new Object[]{Const.RV_PROTOCOL, Const.RV_PROT_HTTPS});
          break;
        case "tcp":
          rd.set(rd.size(), new Object[]{Const.RV_PROTOCOL, Const.RV_PROT_TCP});
          break;
        case "tls":
          rd.set(rd.size(), new Object[]{Const.RV_PROTOCOL, Const.RV_PROT_TLS});
          break;
        case "coap":
          rd.set(rd.size(), new Object[]{Const.RV_PROTOCOL, Const.RV_PROT_COAP_TCP});
          break;
        case "coapudp":
          rd.set(rd.size(), new Object[]{Const.RV_PROTOCOL, Const.RV_PROT_COAP_UDP});
          break;
        default:
          throw new DispatchException(new InvalidParameterException());
      }
      String[] queryParams = uri.getQuery().split("&");
      for (String param : queryParams) {
        String[] pair = param.split("=");
        rd.set(rd.size(), getDirective(pair));
      }

      result.set(result.size(), rd);
    }
    return result;
  }

  /**
   * Query the value of bytearray based RendezvousInfo variable.
   *
   * @param directive     The directive containing RendezvousInfo variables.
   * @param queryVariable The variable id to query.
   * @return The value of the variable if present, otherwise null.
   */
  public static byte[] queryByteArrayValue(Composite directive, int queryVariable) {
    byte[] result = null;
    for (int i = 0; i < directive.size(); i++) {
      Composite variable = directive.getAsComposite(i);
      if (variable.getAsNumber(Const.FIRST_KEY).intValue() == queryVariable) {
        result = variable.getAsBytes(Const.SECOND_KEY);
        break;
      }
    }
    return result;
  }

  /**
   * Query the IpAddress of a RendezvousInfo directive.
   *
   * @param directive The directive containing RendezvousInfo variables.
   * @return The value of the variable if present, otherwise null.
   */
  public static InetAddress queryIpAddressValue(Composite directive) {
    InetAddress result = null;
    for (int i = 0; i < directive.size(); i++) {
      Composite variable = directive.getAsComposite(i);
      if (variable.getAsNumber(Const.FIRST_KEY).intValue() == Const.RV_IP_ADDRESS) {
        try {
          result = InetAddress.getByAddress(variable.getAsBytes(Const.SECOND_KEY));
        } catch (UnknownHostException e) {
          throw new InvalidIpAddressException(e);
        }
        break;
      }
    }
    return result;
  }

  /**
   * Query the value of String based RendezvousInfo variable.
   *
   * @param directive     The directive containing RendezvousInfo variables.
   * @param queryVariable The variable id to query.
   * @return The value of the variable if present, otherwise null.
   */
  public static String queryStringValue(Composite directive, int queryVariable) {
    String result = null;
    for (int i = 0; i < directive.size(); i++) {
      Composite variable = directive.getAsComposite(i);
      if (variable.getAsNumber(Const.FIRST_KEY).intValue() == queryVariable) {
        result = variable.getAsString(Const.SECOND_KEY);
        break;
      }
    }
    return result;
  }

  /**
   * Query the value of Integer based RendezvousInfo variable.
   *
   * @param directive     The directive containing RendezvousInfo variables.
   * @param queryVariable The variable id to query.
   * @return The value of the variable if present, otherwise null.
   */
  public static Integer queryIntValue(Composite directive, int queryVariable) {

    Integer result = null;
    for (int i = 0; i < directive.size(); i++) {
      Composite variable = directive.getAsComposite(i);
      if (variable.getAsNumber(Const.FIRST_KEY).intValue() == queryVariable) {
        result = variable.getAsNumber(Const.SECOND_KEY).intValue();
        break;
      }
    }
    return result;
  }

  /**
   * Gets a List of HTTP directives.
   *
   * @param rvi    A composite with RendezvousInfo.
   * @param filter A filter.
   *               <p>Const.RV_DEV_ONLY</p>
   *               <p>Const.RV_OWNER_ONLY</p>
   * @return A list of HTTP uri's.
   */
  public static List<String> getHttpDirectives(Composite rvi, int filter) {

    List<String> list = new ArrayList<String>();

    for (int i = 0; i < rvi.size(); i++) {

      Composite directive = rvi.getAsComposite(i);

      Integer value = null;
      if (filter == Const.RV_DEV_ONLY) {
        value = queryIntValue(directive, Const.RV_OWNER_ONLY);
        if (value != null) {
          continue;
        }
      } else if (filter == Const.RV_OWNER_ONLY) {
        value = queryIntValue(directive, Const.RV_DEV_ONLY);
        if (value != null) {
          continue;
        }
      } else {
        throw new IllegalArgumentException(Integer.toString(filter));
      }

      value = queryIntValue(directive, Const.RV_PROTOCOL);
      if (value != null
          && (value == Const.RV_PROT_HTTP || value == Const.RV_PROT_HTTPS)) {

        //default http value
        String devPort = "80";
        String ownerPort = devPort;
        String protocol = "http://";

        //override with https
        if (value == Const.RV_PROT_HTTPS) {
          protocol = "https://";
          ownerPort = devPort = "443";
        }

        String portString = null;
        if (filter == Const.RV_DEV_ONLY) {
          Integer portQuery = queryIntValue(directive, Const.RV_DEV_PORT);
          if (portQuery != null) {
            devPort = portQuery.toString();
          }
          portString = devPort;
        } else if (filter == Const.RV_OWNER_ONLY) {
          //override owner port
          Integer portQuery = queryIntValue(directive, Const.RV_OWNER_PORT);
          if (portQuery != null) {
            ownerPort = portQuery.toString();
          }
          portString = ownerPort;
        }

        String hostName = queryStringValue(directive, Const.RV_DNS);
        list.add(protocol + hostName + ":" + portString);

        InetAddress ipAddr = queryIpAddressValue(directive);
        String ipAddrString = ipAddr.getHostAddress();

        list.add(protocol + ipAddrString + ":" + portString);
      }
    }

    return list;

  }
}
