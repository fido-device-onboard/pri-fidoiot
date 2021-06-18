// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

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

        String devPort = "";
        String ownerPort = "";
        String protocol = "";

        if (filter == Const.RV_DEV_ONLY) {
          //Device protocol based on the RVProtocol value.

          //default http value
          devPort = "80";
          ownerPort = devPort;
          protocol = "http://";

          //override with https
          if (value == Const.RV_PROT_HTTPS) {
            protocol = "https://";
            ownerPort = devPort = "443";
          }
        } else if (filter == Const.RV_OWNER_ONLY) {
          //The Owner always chooses HTTPS for communication.
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
