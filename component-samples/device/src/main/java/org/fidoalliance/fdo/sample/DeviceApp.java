// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.DispatchMessage;
import org.fidoalliance.fdo.protocol.HttpClient;
import org.fidoalliance.fdo.protocol.HttpInstruction;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialSupplier;
import org.fidoalliance.fdo.protocol.dispatch.DeviceKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.ExceptionConsumer;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.AppStart;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;
import org.fidoalliance.fdo.protocol.message.HelloDevice;
import org.fidoalliance.fdo.protocol.message.HelloRv;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.Nonce;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.fidoalliance.fdo.protocol.message.SigInfo;
import org.fidoalliance.fdo.protocol.message.SigInfoType;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;
import org.fidoalliance.fdo.protocol.message.To1dPayload;

public class DeviceApp extends HttpClient {

  private static final LoggerService logger = new LoggerService(DeviceApp.class);

  private final DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();


  /**
   * Main entry.
   *
   * @param args Commandline arguments.
   */
  public static void main(String[] args) {
    try {
      new DeviceApp().run();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void run() {

    try {
      logger.info("Starting FDO Device");
      super.run();
    } catch (RuntimeException e) {
      logger.error(e.getMessage());
      logger.info("Exiting FDO Device Application");
    } catch (Throwable throwable) {
      DispatchMessage prevMessage = new DispatchMessage();
      prevMessage.setMsgType(MsgType.DI_APP_START);
      DispatchMessage.fromThrowable(throwable, prevMessage);

      try {
        Config.getWorker(ExceptionConsumer.class).accept(throwable);
      } catch (IOException e) {
        logger.error("failed log exception");
        // already in exception handler
      }
      logger.info("Exiting FDO Device Application");
    }
  }

  @Override
  public void generateHello() throws IOException {

    //store the original message

    SimpleStorage storage = getRequest().getExtra();

    CoseSign1 to1d = storage.get(CoseSign1.class);

    if (to1d != null) {
      generateTo2Hello(to1d);
    } else {
      final DeviceCredential devCredential = Config.getWorker(DeviceCredentialSupplier.class).get();
      if (devCredential == null) {
        generateDiHello();
        logger.info("Generating Device Credential file");
      } else {
        setInstructions(HttpUtils.getInstructions(devCredential.getRvInfo(), true));

        storage.put(DeviceCredential.class, devCredential);
        logger.info("credentials loaded, GUID is " + devCredential.getGuid());
        generateTo1Hello(devCredential);
      }
    }

  }

  @Override
  protected void generateBypass() throws IOException {
    generateTo2Hello(null);
  }

  @Override
  protected void clearByPass() throws IOException {
    getRequest().setMsgType(MsgType.TO1_HELLO_RV);
    DeviceCredential devCredential = getRequest().getExtra().get(DeviceCredential.class);
    generateTo1Hello(devCredential);
  }

  private void generateTo1Hello(DeviceCredential devCredential) throws IOException {

    HelloRv helloRv = new HelloRv();
    helloRv.setGuid(devCredential.getGuid());
    helloRv.setSigInfo(getSignInfo(config.getKeyType()));
    getRequest().setMsgType(MsgType.TO1_HELLO_RV);
    getRequest().setMessage(Mapper.INSTANCE.writeValue(helloRv));

  }

  private void generateTo2Hello(CoseSign1 tod1) throws IOException {

    DeviceCredential cred = Config.getWorker(DeviceCredentialSupplier.class).get();
    HelloDevice helloDevice = new HelloDevice();
    helloDevice.setMaxMessageSize(config.getMaxMessageSize());
    logger.info("max message size is " + config.getMaxMessageSize());

    helloDevice.setGuid(cred.getGuid());
    logger.info("GUID is " + cred.getGuid());
    Nonce nonceTO2ProveOv = Nonce.fromRandomUuid();
    helloDevice.setProveTo2Ov(nonceTO2ProveOv);
    helloDevice.setKexSuiteName(config.getKexSuite());
    helloDevice.setCipherSuiteName(config.getCipherSuite());

    helloDevice.setSigInfo(getSignInfo(config.getKeyType()));

    getRequest().setMsgType(MsgType.TO2_HELLO_DEVICE);
    getRequest().setMessage(Mapper.INSTANCE.writeValue(helloDevice));

    SimpleStorage storage = getRequest().getExtra();
    storage.put(byte[].class, getRequest().getMessage());
    storage.put(Nonce.class, nonceTO2ProveOv);

    if (tod1 != null) {
      To1dPayload to1dPayload = Mapper.INSTANCE.readValue(tod1.getPayload(), To1dPayload.class);
      setInstructions(HttpUtils.getInstructions(to1dPayload.getAddressEntries()));
      storage.put(CoseSign1.class, tod1);
    }

  }

  private void generateDiHello() throws IOException {

    PublicKeyType keyType = config.getKeyType();
    if (!keyType.equals(PublicKeyType.SECP384R1) && !keyType.equals(PublicKeyType.SECP256R1)) {
      throw new InternalServerErrorException(new IllegalArgumentException("invalid key type"));
    }

    KeySizeType keySize = KeySizeType.SIZE_384;
    if (keyType.equals(PublicKeyType.SECP256R1)) {
      keySize = KeySizeType.SIZE_256;
    }

    List<HttpInstruction> httpInst = new ArrayList<>();
    HttpInstruction instruction = new HttpInstruction();
    instruction.setAddress(config.getDiUri());
    httpInst.add(instruction);

    setInstructions(httpInst);

    logger.info("DI URL is " + config.getDiUri());

    String serialNo = Hex.encodeHexString(
        Config.getWorker(CryptoService.class).getRandomBytes(4), false);

    logger.info("Device Serial No:" + serialNo);

    byte[] csr = generateCsr(keyType, keySize);

    ManufacturingInfo mfgInfo = new ManufacturingInfo();
    mfgInfo.setKeyType(keyType);
    mfgInfo.setKeyEnc(config.getKeyEnc());
    mfgInfo.setSerialNumber(serialNo);
    mfgInfo.setCertInfo(AnyType.fromObject(csr));
    mfgInfo.setDeviceInfo("DemoDevice");
    logger.info("Device Info: " + mfgInfo.getDeviceInfo());
    AppStart appStart = new AppStart();
    appStart.setManufacturingInfo(Mapper.INSTANCE.writeValue(mfgInfo));

    setRequest(new DispatchMessage());
    getRequest().setExtra(new SimpleStorage());
    getRequest().setMsgType(MsgType.DI_APP_START);
    getRequest().setMessage(Mapper.INSTANCE.writeValue(appStart));


  }

  private byte[] generateCsr(PublicKeyType keyType, KeySizeType keySize)
      throws IOException {

    PrivateKey signingKey = null;
    final CryptoService cs = Config.getWorker(CryptoService.class);
    try {

      final KeyResolver keyResolver = Config.getWorker(DeviceKeySupplier.class).get();

      final String alias = KeyResolver.getAlias(keyType, keySize);

      signingKey = keyResolver.getPrivateKey(alias);

      final X509Certificate cert = (X509Certificate) keyResolver.getCertificateChain(alias)[0];

      final ContentSigner signer =
          new JcaContentSignerBuilder(cert.getSigAlgName()).build(signingKey);

      final X500Name x500name = new X500Name("CN=Fdo Device");

      final PKCS10CertificationRequestBuilder csrBuilder =
          new JcaPKCS10CertificationRequestBuilder(x500name,
              cert.getPublicKey());

      final PKCS10CertificationRequest pkcs10 = csrBuilder.build(signer);
      return pkcs10.getEncoded();

    } catch (OperatorCreationException e) {
      logger.error("Operator Creation or Certificate Encoding Failed :" + e.getMessage());
      throw new IOException(e);
    } finally {
      cs.destroyKey(signingKey);
    }
  }

  private SigInfo getSignInfo(PublicKeyType keyType) throws InternalServerErrorException {
    SigInfo sigInfo = new SigInfo();
    switch (keyType) {
      case SECP256R1:
        sigInfo.setSigInfoType(SigInfoType.SECP256R1);
        break;
      case SECP384R1:
        sigInfo.setSigInfoType(SigInfoType.SECP384R1);
        break;
      default:
        throw new InternalServerErrorException(new IllegalArgumentException());
    }
    return sigInfo;
  }
}
