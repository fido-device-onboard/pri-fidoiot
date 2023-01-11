package org.fidoalliance.fdo.sample;

import java.util.Arrays;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import org.fidoalliance.fdo.protocol.EnvironmentSanityPredicate;

public class DeviceEnvironmentSanityPredicate implements EnvironmentSanityPredicate {
  @Override
  public boolean test(Map map, String node) throws Exception {

    if (node.equals("device")) {
      try {
        if (map.get("service-info-mtu") != null) {
          int mtuValue = Integer.parseInt(String.valueOf(map.get("service-info-mtu")));
          if (mtuValue < 1300 || mtuValue > 65536) {
            throw new InvalidPropertiesFormatException("Invalid serviceInfo mtu range.");
          }
        }
      } catch (NullPointerException e) {
        throw new NullPointerException("service-info-mtu is set to null in service.yml");
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Invalid service-info-mtu in service.yml");
      }

      try {
        if (map.get("max-message-size") != null) {
          int mtuValue = Integer.parseInt(String.valueOf(map.get("max-message-size")));
          if (mtuValue < 0 || mtuValue > 100000) {
            throw new InvalidPropertiesFormatException("Invalid max-message-size range.");
          }
        }
      } catch (NullPointerException e) {
        throw new NullPointerException("max-message-size is set to null in service.yml");
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Invalid max-message-size in service.yml");
      }

      try {
        if (map.get("di-url") == null
            || map.get("di-url").toString().toLowerCase().equals("null")) {
          throw new NullPointerException("di-url property is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException("di-url property is null in service.yml");
      }

      try {
        if (map.get("credential-file") == null
            || map.get("credential-file").toString().toLowerCase().equals("null")) {
          throw new NullPointerException("credential-file property" + " is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException("credential-file property" + " is null in service.yml");
      }

      try {
        if (map.get("key-exchange-suite") != null) {
          List<String> supportedValues =
              Arrays.asList("ECDH384", "ECDH256", "ASYMKEX3072", "ASYMKEX2048");
          if (supportedValues.stream()
                  .filter(suite -> suite.equals(map.get("key-exchange-suite")))
                  .count()
              != 1) {
            throw new IllegalArgumentException(
                "Unsupported key-exchange-suite" + " in service.yml");
          }
        } else {
          throw new NullPointerException("key-exchange-suite is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException(
            "key-exchange-suite property is " + "null in service.yml");
      }

      try {
        if (map.get("cipher-suite") != null) {
          List<Integer> supportedValues = Arrays.asList(1, 3, 32, 33);
          if (supportedValues.stream()
                  .filter(suite -> suite.equals(map.get("cipher-suite")))
                  .count()
              != 1) {
            throw new IllegalArgumentException("Unsupported cipher-suite in" + " service.yml");
          }
        } else {
          throw new NullPointerException("cipher-suite is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException("cipher-suite property is null in service.yml");
      }

      try {
        if (map.get("key-enc") != null) {
          List<Integer> supportedValues = Arrays.asList(1, 2, 3);
          if (supportedValues.stream().filter(suite -> suite.equals(map.get("key-enc"))).count()
              != 1) {
            throw new IllegalArgumentException("Unsupported key-encoding in service.yml");
          }
        } else {
          throw new NullPointerException("key-enc is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException("key-enc property is null in service.yml");
      }

      try {
        if (map.get("key-type") != null) {
          List<Integer> supportedValues = Arrays.asList(1, 5, 10, 11);
          if (supportedValues.stream().filter(suite -> suite.equals(map.get("key-type"))).count()
              != 1) {
            throw new IllegalArgumentException("Unsupported key-type in service.yml");
          }
        } else {
          throw new NullPointerException("key-type is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException("key-type property is null in service.yml");
      }
    }

    return true;
  }
}
