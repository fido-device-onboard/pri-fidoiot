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

  /**
   * Performs sanity checks on the CBOR RvInfo.
   *
   * @param rvi Composite object of the CBOR RvInfo
   * @return whether CBOR RvInfo passes the sanity checks.
   */
  public static boolean sanityCheck(Composite rvi) {
    try {
      for (int i = 0; i < rvi.size(); i++) {
        Composite directive = rvi.getAsComposite(i);
        for (int index = 0; index < directive.size(); index++) {
          List<Object> item = (List<Object>) directive.get(index);
          long rvVariable = (long) item.get(0);
          if (rvVariable < Const.RV_DEV_ONLY || rvVariable > Const.RV_EXTRV) {
            //Out of range rvVariable. Valid range is between 0-15.
            System.out.println("Invalid RvVariable provided.");
            return false;
          } else if (rvVariable == Const.RV_DEV_ONLY || rvVariable == Const.RV_OWNER_ONLY
                  || rvVariable == Const.RV_BYPASS) {
            // For these 3 RV Variables, ensure that only one item is present.
            if (item.size() > 1) {
              return false;
            }
          } else if (rvVariable == Const.RV_IP_ADDRESS) {
            //Ensure that proper ip address is resolved.
            queryIpAddressValue(directive);
          } else if (rvVariable == Const.RV_DEV_PORT) {
            Integer devport = queryIntValue(directive, Const.RV_DEV_PORT);
            //Ensure that devport is in the valid range of (0,65535).
            if (devport < 0 || devport > 65535) {
              System.out.println("Invalid devport.");
              return false;
            }
          } else if (rvVariable == Const.RV_OWNER_PORT) {
            Integer ownerport = queryIntValue(directive, Const.RV_OWNER_PORT);
            //Ensure that ownerport is in the valid range of (0,65535).
            if (ownerport < 0 || ownerport > 65535) {
              System.out.println("Invalid Ownerport.");
              return false;
            }
          } else if (rvVariable == Const.RV_SV_CERT_HASH) {
            //TODO: Decode and check the structure of Hash.
          } else if (rvVariable == Const.RV_CLT_CERT_HASH) {
            //TODO: Decode and check the structure of Hash.
          } else if (rvVariable == Const.RV_USER_INPUT) {
            //Ensure that user input is a boolean value.
            getBoolean((String) item.get(1));
          } else if (rvVariable == Const.RV_WIFI_SSID) {
            queryStringValue(directive,Const.RV_WIFI_SSID);
          } else if (rvVariable == Const.RV_WIFI_PW) {
            queryStringValue(directive,Const.RV_WIFI_PW);
          } else if (rvVariable == Const.RV_MEDIUM) {
            Integer medium = queryIntValue(directive, Const.RV_DEV_PORT);
            //Ensure that medium is in the valid range of (0,21).
            if (medium < Const.RV_MED_ETH0 || medium > Const.RV_MED_WIFI_ALL) {
              System.out.println("Invalid medium used.");
              return false;
            }
          } else if (rvVariable == Const.RV_PROTOCOL) {
            int value = queryIntValue(directive, Const.RV_PROTOCOL);
            //Ensure that protocol value is in the valid range of (0,6).
            if (value < Const.RV_PROT_REST || value > Const.RV_PROT_COAP_UDP) {
              System.out.println("Invalid Protocol used.");
              return false;
            }
          } else if (rvVariable == Const.RV_DELAY_SEC) {
            int value = queryIntValue(directive, Const.RV_DELAY_SEC);
            //Ensuring that RV_DELAY_SEC is not a negative value.
            if (value < 0) {
              return false;
            }
          }
        }
      }
    } catch (InvalidIpAddressException e) {
      System.out.println("Invalid IP address provided.");
      return false;
    } catch (MessageBodyException e) {
      System.out.println("Invalid WiFi SSID/ WiFi PW provided.");
      return false;
    } catch (DispatchException e) {
      System.out.println("Invalid user inpur provided.");
    } catch (Exception e) {
      return false;
    }

    return true;
  }
}
