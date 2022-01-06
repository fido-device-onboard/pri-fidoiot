package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.utils.URIBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.HttpClient;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.DeviceKeySupplier;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.AppStart;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolInfo;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialSupplier;
import org.fidoalliance.fdo.protocol.dispatch.SessionManager;

public class DeviceApp extends HttpClient {

  private final LoggerService logger;
  private final DeviceConfig config;

  public DeviceApp() throws IOException {
    logger = new LoggerService(DeviceApp.class);
    config =  Config.getConfig(RootConfig.class).getRoot();
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

    /**
     *
     * TODO: if owner sends unsupported module message return 255
     */


    logger.info("Starting Fdo Device");
    super.run();
    logger.info("Starting Fdo Completed");
  }

  @Override
  public void generateHello() throws IOException {

    //reset token casl

    DeviceCredential devCredential = Config.getWorker(DeviceCredentialSupplier.class).get();
    SessionManager session = Config.getWorker(MemoryResidentSessionManager.class);

    //String token = getRequest().getProtocolInfo().getAuthToken();
    //if (token != null) {

    //}

    //if (session.)
    //if (credFile.exists()) {
    //  generateTo1Hello();
    //} else {
    generateDiHello();
    // }
  }

  private void generateTo1Hello() {

  }

  private void generateDiHello() throws IOException {

    KeyPair pair = null;
    try {


      PublicKeyType keyType = config.getKeyType();
      if (!keyType.equals(PublicKeyType.SECP384R1) && !keyType.equals(PublicKeyType.SECP256R1)) {
        throw new InternalServerErrorException(new IllegalArgumentException("invalid key type"));
      }

      KeySizeType keySize = KeySizeType.SIZE_384;
      if (keyType.equals(PublicKeyType.SECP256R1)) {
        keySize = KeySizeType.SIZE_256;
      }

      setServerUri(config.getDiUri());
      URIBuilder uriBuilder = new URIBuilder(getServerUri());
      List<String> segList = new ArrayList<>();
      segList.add(HttpUtils.FDO_COMPONENT);
      segList.add(ProtocolVersion.current().toString());
      segList.add(HttpUtils.MSG_COMPONENT);
      segList.add(MsgType.DI_APP_START.toString());
      uriBuilder.setPathSegments(segList);
      setRequest(HttpUtils.getMessageFromURI(uriBuilder.build().getPath()));


      byte[] csr = generateCsr(keyType,keySize);

      String serialNo = Hex.encodeHexString(
          Config.getWorker(CryptoService.class).getRandomBytes(4), false);

      ManufacturingInfo mfgInfo = new ManufacturingInfo();
      mfgInfo.setKeyType(keyType);
      mfgInfo.setKeyEnc(config.getKeyEnc());
      mfgInfo.setSerialNumber(serialNo);
      mfgInfo.setCertInfo(AnyType.fromObject(csr));
      mfgInfo.setDeviceInfo("DemoDevice");

      AppStart appStart = new AppStart();
      AnyType mfgInfoItem = AnyType.fromObject(mfgInfo);
      mfgInfoItem.wrap();
      appStart.setManufacturingInfo(mfgInfoItem);
      getRequest().setMessage(AnyType.fromObject(appStart));

    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private byte[] generateCsr(PublicKeyType keyType, KeySizeType keySize)
      throws IOException {

    PrivateKey signingKey = null;
    final CryptoService cs = Config.getWorker(CryptoService.class);
    try {


      final KeyResolver keyResolver = Config.getWorker(DeviceKeySupplier.class).get();

      final String alias =  KeyResolver.getAlias(keyType,keySize);

      signingKey  = (PrivateKey) keyResolver.getPrivateKey(alias);

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


}
