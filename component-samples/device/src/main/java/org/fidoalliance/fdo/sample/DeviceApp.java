package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
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
import org.fidoalliance.fdo.protocol.dispatch.DeviceKeySupplier;
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
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialSupplier;
import org.fidoalliance.fdo.protocol.message.SigInfo;
import org.fidoalliance.fdo.protocol.message.SigInfoType;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;
import org.fidoalliance.fdo.protocol.message.To1dPayload;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.fidoalliance.fdo.protocol.message.To2AddressEntry;

public class DeviceApp extends HttpClient {

  private final LoggerService logger;
  private final DeviceConfig config;

  public DeviceApp() throws IOException {
    logger = new LoggerService(DeviceApp.class);
    config = Config.getConfig(RootConfig.class).getRoot();
  }

  public static void main(String[] args) {
    try {
      new DeviceApp().run();
    } catch (IOException e) {
      new RuntimeException(e);
    }
  }


  @Override
  public void run() {

    try {
      logger.info("Starting Fdo Device");
      super.run();
      logger.info("Starting Fdo Completed");
    } catch (Throwable throwable) {
      logger.error(throwable);
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
      } else {
        logger.info("credentials loaded, GUID is " + devCredential.getGuid());
        generateTo1Hello(devCredential);
      }
    }

  }

  @Override
  protected void generateBypass() throws IOException {
    generateTo2Hello(null);
  }

  private void generateTo1Hello(DeviceCredential devCredential) throws IOException {

    setInstructions(HttpUtils.getInstructions(devCredential.getRvInfo(), true));

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
    helloDevice.setGuid(cred.getGuid());
    Nonce nonceTO2ProveOv = Nonce.fromRandomUUID();
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

    byte[] csr = generateCsr(keyType, keySize);

    String serialNo = Hex.encodeHexString(
        Config.getWorker(CryptoService.class).getRandomBytes(4), false);

    logger.info("Device Serial No:" + serialNo);

    ManufacturingInfo mfgInfo = new ManufacturingInfo();
    mfgInfo.setKeyType(keyType);
    mfgInfo.setKeyEnc(config.getKeyEnc());
    mfgInfo.setSerialNumber(serialNo);
    mfgInfo.setCertInfo(AnyType.fromObject(csr));
    mfgInfo.setDeviceInfo("DemoDevice");

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

      signingKey = (PrivateKey) keyResolver.getPrivateKey(alias);

      final X509Certificate cert = (X509Certificate) keyResolver.getCertificateChain(alias)[0];

      final ContentSigner signer =
          new JcaContentSignerBuilder(cert.getSigAlgName()).build(signingKey);

      final X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();

      final PKCS10CertificationRequestBuilder csrBuilder =
          new JcaPKCS10CertificationRequestBuilder(x500name,
              cert.getPublicKey());

      final PKCS10CertificationRequest pkcs10 = csrBuilder.build(signer);
      return pkcs10.getEncoded();

    } catch (OperatorCreationException | CertificateEncodingException e) {
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
