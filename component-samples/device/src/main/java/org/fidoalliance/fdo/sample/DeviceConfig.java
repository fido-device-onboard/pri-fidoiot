// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.message.CipherSuiteType;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

public class DeviceConfig {

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

  private int resolveInt(String value) {
    return Integer.parseInt(Config.resolve(value));
  }

  public String getCredentialFile() {
    return Config.resolvePath(credentialFile);
  }

  public KeyStoreConfig getKeyStoreConfig() {
    return keystoreConfig;
  }

  public boolean getReuseSupported() {
    return Boolean.getBoolean(resolve(reuseSupported));
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

  public int getMaxMessageSize() {
    return resolveInt(maxMessageSize);
  }

  public int getSviMtu() {
    return resolveInt(sviMtu);
  }

  public String getDiUri() {
    return resolve(diUri);
  }

}