// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.message.CipherSuiteType;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

public class DeviceConfig {

  LoggerService logger =  new LoggerService(DeviceConfig.class);

  @JsonProperty("keystore")
  private KeyStoreConfig keystoreConfig;

  @JsonProperty("credential-file")
  private String credentialFile;

  @JsonProperty("credential-reuse")
  private String reuseSupported;

  @JsonProperty("key-type")
  private String keyType;

  @JsonProperty("key-len")
  private String keyLen;

  @JsonProperty("key-enc")
  private String keyEnc;

  @JsonProperty("cipher-suite")
  private String cipherSuite;

  @JsonProperty("key-exchange-suite")
  private String kexSuite;

  @JsonProperty("di-url")
  private String diUri;

  @JsonProperty("max-message-size")
  private String maxMessageSize;

  @JsonProperty("service-info-mtu")
  private String sviMtu;

  private String resolve(String value) {
    return Config.resolve(value);
  }

  private Integer resolveInt(String value) {
    if (value != null) {
      return Integer.parseInt(Config.resolve(value));
    }
    return null;
  }

  public String getCredentialFile() {
    return Config.resolvePath(credentialFile);
  }

  public KeyStoreConfig getKeyStoreConfig() {
    return keystoreConfig;
  }

  public PublicKeyType getKeyType() {
    return PublicKeyType.valueOf(resolve(keyType));
  }

  public PublicKeyEncoding getKeyEnc() {
    return PublicKeyEncoding.valueOf(resolve(keyEnc));
  }

  public CipherSuiteType getCipherSuite() {
    return CipherSuiteType.valueOf(resolve(cipherSuite));
  }

  public String getKexSuite() {
    return resolve(kexSuite);
  }

  /**
   * Returns the MaxMessageSize set in service.yml file.
   * @return maxMessageSize
   */
  public int getMaxMessageSize() {
    int value = 0;
    try {
      value = resolveInt(maxMessageSize);
      if (value < 0) {
        logger.error("maxMessageSize less than 0. Defaulting maxMessageSize to 0.");
        value = 0;
      }
    } catch (NumberFormatException e) {
      logger.error("Invalid maxMessageSize. Defaulting maxMessageSize to 0.");
    }
    return value;
  }


  /**
   * Returns the maxSviSize set in service.yml file.
   * @return maxSviSize
   */
  public int getSviMtu() {
    int value = 1300;
    try {
      value = resolveInt(sviMtu);
      if (value < 256) {
        logger.error("service-info-mtu less than 256. Defaulting service-info-mtu to 256.");
        value = 256;
      }
    } catch (NumberFormatException e) {
      logger.error("Invalid service-info-mtu value. Defaulting service-info-mtu to 1300.");
    }
    return value;
  }

  public String getDiUri() {
    return resolve(diUri);
  }

}
