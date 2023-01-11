package org.fidoalliance.fdo.protocol;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class StandardEnvironmentSanityPredicate implements EnvironmentSanityPredicate {
  @Override
  public boolean test(Map map, String node) throws Exception {
    if (node.equals("system-properties")) {
      try {
        if (map.get("log4j.configurationFile") == null
            || map.get("log4j.configurationFile").toString().toLowerCase().equals("null")) {
          throw new NullPointerException();
        }
      } catch (NullPointerException e) {
        throw new NullPointerException("log4j.configurationFile property is null in service.yml");
      }

      try {
        Path path = Path.of((String) map.get("app-data.dir"));
        if (!Files.exists(path)) {
          throw new NullPointerException();
        }
      } catch (NullPointerException e) {
        throw new NullPointerException(
            "app-data.dir value points to non-existent directory in service.yml");
      } catch (SecurityException e) {
        throw new SecurityException(
            "Application doesn't have enough permission to read" + " the app-data directory");
      }

      try {
        if (map.get("application.version") == null
            || map.get("application.version").toString().toLowerCase().equals("null")) {
          throw new NullPointerException("application.version property is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException("application.version property is null in service.yml");
      }
    } else if (node.equals("http-server")) {

      try {
        Path path = Path.of((String) map.get("base_path"));
        if (!Files.exists(path)) {
          throw new NullPointerException();
        }
      } catch (NullPointerException e) {
        throw new NullPointerException("base_path value points to invalid address in service.yml");
      } catch (SecurityException e) {
        throw new SecurityException(
            "Application doesn't have enough permission to read" + " the base_path address");
      }

      try {
        int httpPort = Integer.parseInt((String) map.get("http_port"));
        int httpsPort = Integer.parseInt((String) map.get("https_port"));

        if (httpPort < 0 || httpPort > 65536 || httpsPort < 0 || httpsPort > 65536) {
          throw new IllegalArgumentException("Invalid http/https port range.");
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid value provided in http/https port of http-server section");
      } catch (NullPointerException e) {
        throw new NullPointerException("http/https port value is null in service.yml");
      }

      try {
        if (map.get("http_schemes") == null || map.size() == 0) {
          throw new NullPointerException("http_schemes property is null in service.yml");
        }
        List<String> schemes = ((List<String>) map.get("http_schemes"));
        if (!(schemes.contains("http") || schemes.contains("https"))) {
          throw new IllegalArgumentException("http_schemes doesn't contain any valid scheme");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException("http_schemes property is null in service.yml");
      }

      try {
        int httpTimeout = Integer.parseInt((String) map.get("http_timeout"));
        if (httpTimeout < 0 || httpTimeout > 1000000) {
          throw new IllegalArgumentException("Invalid http_timeout range.");
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid value provided in http_timeout of http-server section");
      } catch (NullPointerException e) {
        throw new NullPointerException("http_timeout value is null in service.yml");
      }
      return true;
    } else if (node.equals("hibernate-properties")) {
      try {
        if (map.get("hibernate.connection.username") == null
            || map.get("hibernate.connection.username").toString().toLowerCase().equals("null")) {
          throw new NullPointerException(
              "hibernate.connection.username property is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException(
            "hibernate.connection.username property is null in service.yml");
      }

      try {
        if (map.get("hibernate.connection.password") == null
            || map.get("hibernate.connection.password").toString().toLowerCase().equals("null")) {
          throw new NullPointerException(
              "hibernate.connection.password property is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException(
            "hibernate.connection.password property is null in service.yml");
      }

      try {
        if (map.get("hibernate.connection.url") == null
            || map.get("hibernate.connection.url").toString().toLowerCase().equals("null")) {
          throw new NullPointerException(
              "hibernate.connection.url property is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException(
            "hibernate.connection.url property is null in service.yml");
      }

      try {
        if (map.get("hibernate.dialect") == null
            || map.get("hibernate.dialect").toString().toLowerCase().equals("null")) {
          throw new NullPointerException("hibernate.dialect property is null in service.yml");
        }
      } catch (NullPointerException e) {
        throw new IllegalArgumentException("hibernate.dialect property is null in service.yml");
      }
    }

    return true;
  }
}
