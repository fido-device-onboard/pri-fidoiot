// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.db.FdoSysModuleExtra;
import org.fidoalliance.fdo.protocol.db.OnboardConfigSupplier;
import org.fidoalliance.fdo.protocol.dispatch.CertSignatureFunction;
import org.fidoalliance.fdo.protocol.dispatch.CredReuseFunction;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.CwtKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialConsumer;
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialSupplier;
import org.fidoalliance.fdo.protocol.dispatch.DeviceKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.HmacFunction;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.MaxServiceInfoSupplier;
import org.fidoalliance.fdo.protocol.dispatch.MessageDispatcher;
import org.fidoalliance.fdo.protocol.dispatch.OwnerInfoSizeSupplier;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.RendezvousAcceptFunction;
import org.fidoalliance.fdo.protocol.dispatch.RendezvousInfoSupplier;
import org.fidoalliance.fdo.protocol.dispatch.ReplacementKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.ReplacementVoucherStorageFunction;
import org.fidoalliance.fdo.protocol.dispatch.RvBlobQueryFunction;
import org.fidoalliance.fdo.protocol.dispatch.RvBlobStorageFunction;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoDocumentSupplier;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.dispatch.SessionManager;
import org.fidoalliance.fdo.protocol.dispatch.VoucherQueryFunction;
import org.fidoalliance.fdo.protocol.dispatch.VoucherReplacementFunction;
import org.fidoalliance.fdo.protocol.dispatch.VoucherStorageFunction;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.AppStart;
import org.fidoalliance.fdo.protocol.message.CertChain;
import org.fidoalliance.fdo.protocol.message.CoseProtectedHeader;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.CoseUnprotectedHeader;
import org.fidoalliance.fdo.protocol.message.CwtTo1Id;
import org.fidoalliance.fdo.protocol.message.CwtToken;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;
import org.fidoalliance.fdo.protocol.message.DeviceServiceInfo;
import org.fidoalliance.fdo.protocol.message.DiDone;
import org.fidoalliance.fdo.protocol.message.EatPayloadBase;
import org.fidoalliance.fdo.protocol.message.EncryptionState;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.HelloDevice;
import org.fidoalliance.fdo.protocol.message.HelloRv;
import org.fidoalliance.fdo.protocol.message.HelloRvAck;
import org.fidoalliance.fdo.protocol.message.KexMessage;
import org.fidoalliance.fdo.protocol.message.KexParty;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.Nonce;
import org.fidoalliance.fdo.protocol.message.NullValue;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.OwnerServiceInfo;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherEntries;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherEntryPayload;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.fidoalliance.fdo.protocol.message.RendezvousConstant;
import org.fidoalliance.fdo.protocol.message.RendezvousDirective;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.fidoalliance.fdo.protocol.message.RendezvousInstruction;
import org.fidoalliance.fdo.protocol.message.RendezvousProtocol;
import org.fidoalliance.fdo.protocol.message.RendezvousVariable;
import org.fidoalliance.fdo.protocol.message.ServiceInfo;
import org.fidoalliance.fdo.protocol.message.ServiceInfoDocument;
import org.fidoalliance.fdo.protocol.message.ServiceInfoGlobalState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleList;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.message.SetCredentials;
import org.fidoalliance.fdo.protocol.message.SetHmac;
import org.fidoalliance.fdo.protocol.message.SigInfo;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;
import org.fidoalliance.fdo.protocol.message.To0AcceptOwner;
import org.fidoalliance.fdo.protocol.message.To0HelloAck;
import org.fidoalliance.fdo.protocol.message.To0OwnerSign;
import org.fidoalliance.fdo.protocol.message.To0OwnerSign2;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To1dPayload;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.fidoalliance.fdo.protocol.message.To2DeviceInfoReady;
import org.fidoalliance.fdo.protocol.message.To2Done;
import org.fidoalliance.fdo.protocol.message.To2Done2;
import org.fidoalliance.fdo.protocol.message.To2GetNextEntry;
import org.fidoalliance.fdo.protocol.message.To2NextEntry;
import org.fidoalliance.fdo.protocol.message.To2OwnerInfoReady;
import org.fidoalliance.fdo.protocol.message.To2ProveDevicePayload;
import org.fidoalliance.fdo.protocol.message.To2ProveHeaderPayload;
import org.fidoalliance.fdo.protocol.message.To2RedirectEntry;
import org.fidoalliance.fdo.protocol.message.To2SetupDevicePayload;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;
import org.fidoalliance.fdo.protocol.serviceinfo.StandardServiceInfoSendFunction;

public class StandardMessageDispatcher implements MessageDispatcher {

  private static final LoggerService logger = new LoggerService(StandardMessageDispatcher.class);

  protected StandardCryptoService getCryptoService() {
    return Config.getWorker(StandardCryptoService.class);
  }

  protected <T> T getWorker(Class<T> t) {
    return Config.getWorker(t);
  }

  protected List<Object> getWorkers() {
    return Config.getWorkers();
  }

  protected ServiceInfoModule getModule(String name) throws IOException {
    List<Object> workers = getWorkers();
    for (Object worker : workers) {
      //build the initial state for all modules
      if (worker instanceof ServiceInfoModule) {
        ServiceInfoModule module = (ServiceInfoModule) worker;
        if (module.getName().equals(name)) {
          return module;
        }
      }
    }
    throw new InternalServerErrorException("missing module: " + name);
  }

  protected String createSessionId() {
    return Hex.encodeHexString(getCryptoService().getRandomBytes(Long.BYTES * 2));
  }


  protected String createCwtSession(CwtTo1Id cwtTo1Id) throws IOException {
    final CwtToken cwtToken = new CwtToken();
    cwtToken.setIssuer("rvs");
    cwtToken.setSubject("to1");
    cwtToken.setAudience("fdo");
    final long time = Duration.ofMillis(System.currentTimeMillis()).toSeconds();
    cwtToken.setIssuedAt(time);
    cwtToken.setNotBefore(time);
    cwtToken.setExpiry(time + Duration.ofMinutes(1).toSeconds());
    cwtToken.setCwtId(Mapper.INSTANCE.writeValue(cwtTo1Id));

    final byte[] payload = Mapper.INSTANCE.writeValue(cwtToken);

    final KeyResolver resolver = Config.getWorker(CwtKeySupplier.class).get();
    final String alias = KeyResolver.getAlias(PublicKeyType.SECP384R1, KeySizeType.SIZE_384);

    OwnerPublicKey pubKey = getCryptoService().encodeKey(PublicKeyType.SECP384R1,
        PublicKeyEncoding.X509,
        resolver.getCertificateChain(alias));
    PrivateKey privateKey = resolver.getPrivateKey(alias);
    try {
      CoseSign1 sign1 = getCryptoService().sign(payload, privateKey, pubKey);
      byte[] data = Mapper.INSTANCE.writeValue(sign1);
      String token = Base64.getEncoder().encodeToString(data);
      return "Bearer " + token;
    } finally {
      getCryptoService().destroyKey(privateKey);
    }

  }


  protected String createCwtSession(Nonce nonce) throws IOException {
    final CwtToken cwtToken = new CwtToken();
    cwtToken.setIssuer("rvs");
    cwtToken.setSubject("to0");
    cwtToken.setAudience("fdo");
    final long time = Duration.ofMillis(System.currentTimeMillis()).toSeconds();
    cwtToken.setIssuedAt(time);
    cwtToken.setNotBefore(time);
    cwtToken.setExpiry(time + Duration.ofMinutes(1).toSeconds());
    cwtToken.setCwtId(nonce.getNonce());

    final byte[] payload = Mapper.INSTANCE.writeValue(cwtToken);

    final KeyResolver resolver = Config.getWorker(CwtKeySupplier.class).get();
    final String alias = KeyResolver.getAlias(PublicKeyType.SECP384R1, KeySizeType.SIZE_384);

    OwnerPublicKey pubKey = getCryptoService().encodeKey(PublicKeyType.SECP384R1,
        PublicKeyEncoding.X509,
        resolver.getCertificateChain(alias));
    PrivateKey privateKey = resolver.getPrivateKey(alias);
    try {
      CoseSign1 sign1 = getCryptoService().sign(payload, privateKey, pubKey);
      byte[] data = Mapper.INSTANCE.writeValue(sign1);
      String token = Base64.getEncoder().encodeToString(data);
      return HttpUtils.HTTP_BEARER + token;
    } finally {
      getCryptoService().destroyKey(privateKey);
    }

  }

  protected CwtToken getCwtSession(String authHeader) throws IOException {

    String token = authHeader.substring(HttpUtils.HTTP_BEARER.length());
    byte[] data = Base64.getDecoder().decode(token);
    CoseSign1 sign1 = Mapper.INSTANCE.readValue(data, CoseSign1.class);

    CwtToken cwtToken = Mapper.INSTANCE.readValue(sign1.getPayload(), CwtToken.class);

    final KeyResolver resolver = Config.getWorker(CwtKeySupplier.class).get();
    final String alias = KeyResolver.getAlias(PublicKeyType.SECP384R1, KeySizeType.SIZE_384);
    OwnerPublicKey pubKey = getCryptoService().encodeKey(PublicKeyType.SECP384R1,
        PublicKeyEncoding.X509,
        resolver.getCertificateChain(alias));

    boolean verified = getCryptoService().verify(sign1, pubKey);
    if (!verified) {
      throw new InvalidMessageException("invalid signature");
    }

    Date now = new Date(System.currentTimeMillis());
    Date expiry = new Date(Duration.ofSeconds(cwtToken.getExpiry()).toMillis());
    if (now.after(expiry)) {
      throw new InvalidJwtTokenException("session expired");
    }

    return cwtToken;
  }

  protected SimpleStorage createVoucher(ManufacturingInfo diInfo, ProtocolVersion version)
      throws IOException {

    final OwnershipVoucher voucher = new OwnershipVoucher();
    final OwnershipVoucherHeader header = new OwnershipVoucherHeader();
    final CryptoService cs = getCryptoService();
    final Guid guid = Guid.fromRandomUuid();

    header.setVersion(version);
    header.setGuid(guid);
    header.setDeviceInfo(diInfo.getDeviceInfo());

    final AnyType certInfo = diInfo.getCertInfo();
    if (certInfo != null || diInfo.getOnDieDeviceCertChain() != null) {
      try {
        Certificate[] chain = null;
        if (diInfo.getOnDieDeviceCertChain() != null) {
          OnDieCertSignatureFunction onDieSigFunction = new OnDieCertSignatureFunction();
          // OnDie ECDSA type device
          chain = onDieSigFunction.apply(diInfo);

          // verify ondie revocations
          if (!onDieSigFunction.checkRevocations(chain)) {
            throw new IOException("OnDie revocation failure");
          }

          // verify test signature
          // First, create proper signed data format
          // Note: value in third_key is empty since
          // we have no nonce with the test signature.
          AlgorithmFinder finder = new AlgorithmFinder();
          CoseProtectedHeader cph = new CoseProtectedHeader();
          cph.setAlgId(finder.getCoseAlgorithm(PublicKeyType.SECP384R1,
              KeySizeType.SIZE_384));
          byte[] cphData = Mapper.INSTANCE.writeValue(cph);

          CoseUnprotectedHeader cuh = new CoseUnprotectedHeader();
          cuh.setMaroPrefix(diInfo.getTestSigMaroePrefix());

          CoseSign1 coseSign = new CoseSign1();
          coseSign.setProtectedHeader(cphData);
          coseSign.setPayload(diInfo.getSerialNumber().getBytes(StandardCharsets.UTF_8));
          coseSign.setSignature(diInfo.getTestSignature());
          coseSign.setUnprotectedHeader(cuh);

          OwnerPublicKey ownerKey = new OwnerPublicKey();
          ownerKey.setBody(AnyType.fromObject(chain[0].getPublicKey().getEncoded()));
          ownerKey.setEnc(PublicKeyEncoding.X509);
          ownerKey.setType(PublicKeyType.SECP384R1);

          if (!cs.verify(coseSign, ownerKey)) {
            throw new InvalidMessageException("OnDie test signature failure.");
          }

        } else {
          // ECDSA type device
          chain = getWorker(CertSignatureFunction.class).apply(diInfo);
        }

        int totalBytes = 0;
        for (Certificate cert : chain) {
          totalBytes += cert.getEncoded().length;
        }

        final ByteBuffer buff = ByteBuffer.allocate(totalBytes);
        for (int i = 0; i < chain.length; i++) {
          buff.put(chain[i].getEncoded());
        }
        buff.flip();

        buff.clear();

        final List<Certificate> certList = new ArrayList<>(Arrays.asList(chain));
        voucher.setCertChain(CertChain.fromList(certList));

        final KeyResolver keyResolver = getWorker(ManufacturerKeySupplier.class).get();
        final String alias = KeyResolver.getAlias(diInfo.getKeyType(),
            new AlgorithmFinder().getKeySizeType(chain[0].getPublicKey()));

        OwnerPublicKey firstOwner = cs.encodeKey(
            diInfo.getKeyType(),
            diInfo.getKeyEnc(),
            keyResolver.getCertificateChain(alias));

        header.setPublicKey(firstOwner);

        //set the cert hash
        byte[] data = BufferUtils.unwrap(buff);
        header.setCertHash(
            cs.hash(new AlgorithmFinder().getCompatibleHashType(
                    certList.get(0).getPublicKey()),
                data));

      } catch (CertificateEncodingException e) {
        throw new IOException(e);
      }
    } else {
      // no certInfo provided so assume EPID
      final KeyResolver keyResolver = getWorker(ManufacturerKeySupplier.class).get();

      final String alias = KeyResolver.getAlias(diInfo.getKeyType(), KeySizeType.SIZE_2048);

      final Certificate[] ownerChain = keyResolver.getCertificateChain(alias);
      header.setPublicKey(
          cs.encodeKey(PublicKeyType.RSA2048RESTR, diInfo.getKeyEnc(), ownerChain));
    }
    header.setRendezvousInfo(
        getWorker(RendezvousInfoSupplier.class).get()
    );

    voucher.setEntries(new OwnershipVoucherEntries());
    voucher.setHeader(Mapper.INSTANCE.writeValue(header));
    VoucherUtils.verifyVoucher(voucher);

    SimpleStorage storage = new SimpleStorage();
    storage.put(OwnershipVoucher.class, voucher);
    storage.put(ManufacturingInfo.class, diInfo);
    return storage;
  }

  protected void doAppStart(DispatchMessage request, DispatchMessage response) throws IOException {
    AppStart appStart = request.getMessage(AppStart.class);

    ManufacturingInfo mfgInfo = Mapper.INSTANCE.readValue(appStart.getManufacturingInfo(),
        ManufacturingInfo.class);
    SimpleStorage storage = createVoucher(mfgInfo, request.getProtocolVersion());

    SessionManager manager = getWorker(SessionManager.class);

    OwnershipVoucher voucher = storage.get(OwnershipVoucher.class);

    byte[] headerTag = voucher.getHeader();
    SetCredentials setCredentials = new SetCredentials();
    setCredentials.setVoucherHeader(headerTag);
    response.setMessage(Mapper.INSTANCE.writeValue(setCredentials));

    manager.saveSession(response.getAuthToken().get(),
        storage);

  }

  protected void doSetCredentials(DispatchMessage request, DispatchMessage response)
      throws IOException {

    SetCredentials setCredentials = request.getMessage(SetCredentials.class);
    byte[] headerTag = setCredentials.getVoucherHeader();

    DeviceCredential credential = new DeviceCredential();

    HmacFunction hmacFunction = getWorker(HmacFunction.class);
    Hash hmac = hmacFunction.apply(credential, headerTag);

    SetHmac setHmac = new SetHmac();
    setHmac.setHmac(hmac);
    response.setMessage(Mapper.INSTANCE.writeValue(setHmac));

    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(headerTag, OwnershipVoucherHeader.class);

    credential.setActive(true);
    credential.setGuid(header.getGuid());
    credential.setDeviceInfo(header.getDeviceInfo());
    credential.setProtVer(response.getProtocolVersion());
    credential.setRvInfo(header.getRendezvousInfo());

    CryptoService cs = getCryptoService();
    PublicKey publicKey = cs.decodeKey(header.getPublicKey());
    HashType hashType = new AlgorithmFinder().getCompatibleHashType(hmac.getHashType());
    credential.setPubKeyHash(cs.hash(hashType, publicKey.getEncoded()));

    SimpleStorage storage = request.getExtra();
    storage.put(DeviceCredential.class, credential);

  }

  protected void doSetHmac(DispatchMessage request, DispatchMessage response) throws IOException {

    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = manager.getSession(request.getAuthToken().get());
    OwnershipVoucher voucher = storage.get(OwnershipVoucher.class);
    ManufacturingInfo info = storage.get(ManufacturingInfo.class);
    SetHmac setHmac = request.getMessage(SetHmac.class);
    voucher.setHmac(setHmac.getHmac());

    VoucherStorageFunction storageFunction = getWorker(VoucherStorageFunction.class);
    storageFunction.apply(info.getSerialNumber(), voucher);

    //save the voucher
    response.setMessage(Mapper.INSTANCE.writeValue(new DiDone()));
    manager.expireSession(request.getAuthToken().get());
  }

  protected void doDiDone(DispatchMessage request, DispatchMessage response) throws IOException {

    SimpleStorage storage = request.getExtra();
    DeviceCredential credential = storage.get(DeviceCredential.class);
    DeviceCredentialConsumer consumer = getWorker(DeviceCredentialConsumer.class);
    consumer.accept(credential);
    request.setExtra(new SimpleStorage());
    logger.info("DI complete, GUID is " + credential.getGuid());
    logger.info("===== FDO DI SUCCESS =====");
  }

  protected void doTo0Hello(DispatchMessage request, DispatchMessage response) throws IOException {
    Nonce nonceTO0Sign = Nonce.fromRandomUuid();
    response.setAuthToken(createCwtSession(nonceTO0Sign));

    if (request.getMessage().length != 1) {
      throw new InvalidMessageException("Invalid message for the body");
    }

    To0HelloAck helloAck = new To0HelloAck();
    helloAck.setNonce(nonceTO0Sign);
    response.setMessage(Mapper.INSTANCE.writeValue(helloAck));
  }

  protected void doTo0HelloAck(DispatchMessage request, DispatchMessage response)
      throws IOException {

    To0HelloAck helloAck = request.getMessage(To0HelloAck.class);

    SimpleStorage storage = request.getExtra();
    To0d to0d = storage.get(To0d.class);
    to0d.setNonce(helloAck.getNonce());

    CryptoService cs = Config.getWorker(CryptoService.class);
    byte[] to0dBytes = Mapper.INSTANCE.writeValue(to0d);
    HashType hashType = new AlgorithmFinder().getCompatibleHashType(
        to0d.getVoucher().getHmac().getHashType());
    Hash to0dHash = cs.hash(hashType, to0dBytes);

    To1dPayload to1dPayload = new To1dPayload();
    to1dPayload.setAddressEntries(storage.get(To2AddressEntries.class));
    to1dPayload.setTo1ToTo0Hash(to0dHash);

    OwnerPublicKey lastOwner = VoucherUtils.getLastOwner(to0d.getVoucher());
    KeyResolver resolver = getWorker(OwnerKeySupplier.class).get();
    PrivateKey privateKey = resolver.getPrivateKey(cs.decodeKey(lastOwner));
    try {
      CoseSign1 sign1 = cs.sign(
          Mapper.INSTANCE.writeValue(to1dPayload),
          privateKey,
          lastOwner
      );

      To0OwnerSign ownerSign = new To0OwnerSign();
      ownerSign.setTo0d(to0dBytes);
      ownerSign.setTo1d(sign1);

      //uncomment below to send non conformant message
      //To0OwnerSign2 ownerSign = new To0OwnerSign2();
      //ownerSign.setTo0d(to0d);
      //ownerSign.setTo1d(sign1);

      byte[] byteArray = Mapper.INSTANCE.writeValue(ownerSign);
      System.out.println(Arrays.toString(byteArray));
      response.setMessage(byteArray);

    } finally {
      cs.destroyKey(privateKey);
    }


  }

  protected void doTo0OwnerSign(DispatchMessage request, DispatchMessage response)
      throws IOException {

    CwtToken cwtToken = getCwtSession(request.getAuthToken().get());

    To0d to0d;
    CoseSign1 sign1;
    try {
      To0OwnerSign ownerSign = request.getMessage(To0OwnerSign.class);
      to0d = Mapper.INSTANCE.readValue(ownerSign.getTo0d(), To0d.class);
      sign1 = ownerSign.getTo1d();
    } catch (MessageBodyException e) {
      To0OwnerSign2 ownerSign2 = request.getMessage(To0OwnerSign2.class);
      to0d = ownerSign2.getTo0d();
      sign1 = ownerSign2.getTo1d();
      logger.info("non conformant OwnerSign message received");
    }
    Nonce nonceTO0Sign = new Nonce();
    nonceTO0Sign.setNonce(cwtToken.getCwtId());
    if (!nonceTO0Sign.equals(to0d.getNonce())) {
      throw new InvalidMessageException("NonceTO0Sign does not match");
    }

    OwnershipVoucherHeader ovHeader = Mapper.INSTANCE.readValue(to0d.getVoucher().getHeader(),
        OwnershipVoucherHeader.class);

    if (ovHeader.getGuid().toString().isEmpty()) {
      throw new InvalidMessageException("GUID field in OV Header should not be empty");
    }

    if (to0d.getVoucher().getVersion() != ProtocolVersion.V101) {
      throw new InvalidMessageException("Invalid Protocol Version should only accept 101");
    }

    RendezvousInfo info = ovHeader.getRendezvousInfo();

    boolean isValidRVinfo = false;

    for (RendezvousDirective directive : info) {
      boolean isValidDirective = true;
      String dns = null;
      Integer devPort = Integer.valueOf(80);
      Integer ownerPort = Integer.valueOf(443);

      for (RendezvousInstruction instruction : directive) {

        RendezvousVariable variable = instruction.getVariable();

        if (variable == RendezvousVariable.DNS) {
          dns = Mapper.INSTANCE.readValue(instruction.getValue(), String.class);
          if (dns == null || variable.toInteger() != RendezvousConstant.DNS
                  || dns.isEmpty()) {
            logger.info("Invalid RVDNS value in OV Header, moving to next instruction");
            isValidDirective = false;
            break;
          }
        }

        if (variable == RendezvousVariable.DEV_PORT) {
          devPort = Mapper.INSTANCE.readValue(instruction.getValue(), Integer.class);
          if (devPort == null || variable.toInteger() != RendezvousConstant.DEV_PORT) {
            logger.info("Invalid RVDevPort value in OV Header, moving to next instruction");
            isValidDirective = false;
            break;
          }
        }

        if (variable == RendezvousVariable.PROTOCOL) {
          RendezvousProtocol rvp = Mapper.INSTANCE.readValue(instruction.getValue(),
                RendezvousProtocol.class);
          if (rvp == null || variable.toInteger() != RendezvousConstant.PROTOCOL
                  || rvp.toString().isEmpty()) {
            logger.info("Invalid RVProtocol value in OV Header, moving to next instruction");
            isValidDirective = false;
            break;
          }
        }

        if (variable == RendezvousVariable.OWNER_PORT) {
          ownerPort = Mapper.INSTANCE.readValue(instruction.getValue(), Integer.class);
          if (ownerPort == null || variable.toInteger() != RendezvousConstant.OWNER_PORT) {
            logger.info("Invalid RVOwnerPort value in OV Header, moving to next instruction");
            isValidDirective = false;
            break;
          }
        }
      }
      isValidRVinfo |= isValidDirective;
    }

    if (!isValidRVinfo) {
      throw new InvalidMessageException("Invalid RendezvousInfo in OV Header");
    }

    PublicKeyEncoding mfgPubKeyEnc = ovHeader.getPublicKey().getEnc();
    if (mfgPubKeyEnc.toInteger() < 0 || mfgPubKeyEnc.toInteger() > 3) {
      throw new InvalidMessageException("Invalid Encoding of Mfg Pubkey in OV Header");
    }
    
    //verify to1d
    CryptoService cs = getCryptoService();
    OwnerPublicKey ownerPublicKey = VoucherUtils.getLastOwner(to0d.getVoucher());
    boolean verified = cs.verify(sign1, ownerPublicKey);
    if (!verified) {
      logger.error("Unable to verify Owner signature.");
      throw new InvalidOwnerSignException();
    }

    if (!getWorker(RendezvousAcceptFunction.class).apply(to0d.getVoucher())) {
      throw new InvalidMessageException("Voucher rejected due to untrusted key or guid");
    }

    String guid = Mapper.INSTANCE.readValue(to0d.getVoucher().getHeader(),
        OwnershipVoucherHeader.class).getGuid().toString();

    try {
      To2RedirectEntry storedRedirectEntry = getWorker(RvBlobQueryFunction.class).apply(guid);
      boolean verifiedRedirect = cs.verify(storedRedirectEntry.getTo1d(), ownerPublicKey);
      if (!verifiedRedirect) {
        logger.error("Invalid extension of OwnershipVoucher.");
        throw new InvalidOwnerSignException();
      }
    } catch (ResourceNotFoundException e) {
      // Redirect Blob is not stored for the guid
    }

    VoucherUtils.verifyVoucher(to0d.getVoucher());

    To2RedirectEntry redirectEntry = new To2RedirectEntry();
    redirectEntry.setTo1d(sign1);
    CertChain deviceChain = to0d.getVoucher().getCertChain();
    redirectEntry.setCertChain(deviceChain);
    long waitSeconds = getWorker(RvBlobStorageFunction.class).apply(to0d, redirectEntry);

    To0AcceptOwner acceptOwner = new To0AcceptOwner();

    //get wait seconds response
    acceptOwner.setWaitSeconds(waitSeconds);

    response.setMessage(Mapper.INSTANCE.writeValue(acceptOwner));
  }

  protected void doTo0AcceptOwner(DispatchMessage request, DispatchMessage response)
      throws IOException {

    To0AcceptOwner acceptOwner = request.getMessage(To0AcceptOwner.class);
    request.getExtra().put(To0AcceptOwner.class, acceptOwner);
    if (acceptOwner.getWaitSeconds() < 0) {
      throw new IOException("Invalid waitSeconds");
    }
  }

  protected void doTo1Hello(DispatchMessage request, DispatchMessage response) throws IOException {

    HelloRv helloRv = request.getMessage(HelloRv.class);
    HelloRvAck helloRvAck = new HelloRvAck();

    Nonce nonce = Nonce.fromRandomUuid();
    helloRvAck.setNonceTo1Proof(nonce);

    SigInfo sigInfoB = getCryptoService().getSigInfoB(helloRv.getSigInfo());
    helloRvAck.setSigInfoB(sigInfoB);
    CwtTo1Id cwtTo1Id = new CwtTo1Id();
    cwtTo1Id.setHelloRv(request.getMessage(HelloRv.class));
    cwtTo1Id.setNonce(nonce);
    response.setAuthToken(createCwtSession(cwtTo1Id));
    response.setMessage(Mapper.INSTANCE.writeValue(helloRvAck));
  }

  protected void doTo1HelloAck(DispatchMessage request, DispatchMessage response)
      throws IOException {

    HelloRvAck helloRvAck = request.getMessage(HelloRvAck.class);

    EatPayloadBase eat = new EatPayloadBase();
    eat.setNonce(helloRvAck.getNonceTo1Proof());
    DeviceCredential cred = getWorker(DeviceCredentialSupplier.class).get();
    eat.setGuid(cred.getGuid());

    KeyResolver resolver = Config.getWorker(DeviceKeySupplier.class).get();
    Certificate[] chain = resolver.getCertificateChain();

    OwnerPublicKey pubKey = getCryptoService().encodeKey(
        new AlgorithmFinder().getPublicKeyType(chain[0].getPublicKey()),
        PublicKeyEncoding.X509,
        chain);
    PrivateKey privateKey = resolver.getPrivateKey(chain[0].getPublicKey());
    try {
      byte[] payload = Mapper.INSTANCE.writeValue(eat);
      CoseSign1 sign1 = getCryptoService().sign(payload, privateKey, pubKey);
      response.setMessage(Mapper.INSTANCE.writeValue(sign1));
    } finally {
      getCryptoService().destroyKey(privateKey);
    }
  }

  protected void doTo1ProveRv(DispatchMessage request, DispatchMessage response)
      throws IOException {

    CoseSign1 sign1 = request.getMessage(CoseSign1.class);
    CwtToken cwtToken = getCwtSession(request.getAuthToken().get());

    CwtTo1Id cwtTo1Id = Mapper.INSTANCE.readValue(cwtToken.getCwtId(), CwtTo1Id.class);
    Guid guid = cwtTo1Id.getHelloRv().getGuid();

    To2RedirectEntry entry = getWorker(RvBlobQueryFunction.class).apply(guid.toString());

    OwnerPublicKey pubKey = null;
    CertChain certChain = entry.getCertChain();
    if (certChain != null) {
      pubKey = getCryptoService().encodeKey(
          new AlgorithmFinder().getPublicKeyType(certChain.getChain().get(0).getPublicKey()),
          PublicKeyEncoding.X509,
          certChain.getChain().toArray(Certificate[]::new));
      boolean verified = getCryptoService().verify(sign1, pubKey);
      if (!verified) {
        throw new InvalidMessageException("signature failure");
      }
    } else {
      //we don't have the cert chain so the only way is to verify using sigInfo
      boolean verified = getCryptoService().verify(
          sign1,
          cwtTo1Id.getHelloRv().getSigInfo());
      if (!verified) {
        throw new InvalidOwnerSignException();
      }
    }
    EatPayloadBase eatPayload = Mapper.INSTANCE.readValue(sign1.getPayload(), EatPayloadBase.class);
    if (!cwtTo1Id.getNonce().equals(eatPayload.getNonce())) {
      throw new InvalidMessageException("NonceTO1Proof noes not match");
    }
    response.setMessage(Mapper.INSTANCE.writeValue(entry.getTo1d()));
  }

  protected void doTo1Redirect(DispatchMessage request, DispatchMessage response)
      throws IOException {
    CoseSign1 to1d = request.getMessage(CoseSign1.class);

    SimpleStorage storage = request.getExtra();
    storage.put(CoseSign1.class, to1d);

    To1dPayload to1dPayload = Mapper.INSTANCE.readValue(to1d.getPayload(), To1dPayload.class);
    List<String> httpInstruction = new ArrayList<>();
    for (HttpInstruction instruction : HttpUtils.getInstructions(to1dPayload.getAddressEntries())) {
      httpInstruction.add(instruction.getAddress());
    }
    logger.info("TO1 complete, owner is at " + httpInstruction);

  }

  protected void doHelloDevice(DispatchMessage request, DispatchMessage response)
      throws IOException {
    HelloDevice helloDevice = request.getMessage(HelloDevice.class);

    Guid guid = helloDevice.getGuid();
    OwnershipVoucher voucher = getWorker(VoucherQueryFunction.class).apply(guid.toString());

    if (voucher.getEntries().size() > 255) {
      throw new InvalidMessageException("too many voucher entries");
    }
    VoucherUtils.verifyVoucher(voucher);
    To2ProveHeaderPayload hdrPayload = new To2ProveHeaderPayload();

    hdrPayload.setHeader(voucher.getHeader());
    hdrPayload.setNumEntries(voucher.getEntries().size());
    hdrPayload.setHmac(voucher.getHmac());
    hdrPayload.setNonce(helloDevice.getProveTo2Ov());
    hdrPayload.setSigInfoB(getCryptoService().getSigInfoB(helloDevice.getSigInfo()));
    CryptoService cs = getCryptoService();

    KexMessage kexMessage =
        cs.getKeyExchangeMessage(helloDevice.getKexSuiteName(),
            KexParty.A,
            null);
    hdrPayload.setKexA(kexMessage.getMessage());

    HashType hashType = new AlgorithmFinder().getCompatibleHashType(
        voucher.getHmac().getHashType());
    Hash hash = cs.hash(hashType, request.getMessage());
    hdrPayload.setHelloHash(hash);

    OnboardingConfig onboardConfig = new OnboardConfigSupplier().get();
    if (onboardConfig.getMaxMessageSize() != null) {
      int maxMessageSize = onboardConfig.getMaxMessageSize();
      hdrPayload.setMaxMessageSize(maxMessageSize);
    } else {
      hdrPayload.setMaxMessageSize(0);
    }

    CoseUnprotectedHeader uph = new CoseUnprotectedHeader();
    Nonce deviceNonce = Nonce.fromRandomUuid();
    uph.setCupNonce(deviceNonce); //NonceTO2ProveDv

    OwnerPublicKey ownerPublicKey = VoucherUtils.getLastOwner(voucher);
    uph.setOwnerPublicKey(ownerPublicKey);

    SimpleStorage storage = new SimpleStorage();
    storage.put(OwnershipVoucher.class, voucher);
    storage.put(HelloDevice.class, helloDevice);
    storage.put(KexMessage.class, kexMessage);
    storage.put(To2ProveHeaderPayload.class, hdrPayload);

    //store the tod2 for latter
    To2Done to2Done = new To2Done();
    to2Done.setNonce(deviceNonce);
    storage.put(To2Done.class, to2Done);

    KeyResolver resolver = getWorker(OwnerKeySupplier.class).get();
    PrivateKey privateKey = resolver.getPrivateKey(getCryptoService().decodeKey(ownerPublicKey));
    try {
      byte[] payload = Mapper.INSTANCE.writeValue(hdrPayload);
      CoseSign1 sign1 = cs.sign(payload, privateKey, ownerPublicKey);
      sign1.setUnprotectedHeader(uph);
      response.setMessage(Mapper.INSTANCE.writeValue(sign1));
    } finally {
      cs.destroyKey(privateKey);
    }

    SessionManager manager = getWorker(SessionManager.class);
    manager.saveSession(response.getAuthToken().get(),
        storage);

  }

  protected void doProveOwnerHeader(DispatchMessage request, DispatchMessage response)
      throws IOException {
    CoseSign1 sign1 = request.getMessage(CoseSign1.class);
    CoseUnprotectedHeader uph = sign1.getUnprotectedHeader();
    OwnerPublicKey ownerPublicKey = uph.getOwnerPublicKey();
    CryptoService cs = getCryptoService();

    boolean verify = cs.verify(sign1, ownerPublicKey);
    if (!verify) {
      throw new InvalidMessageException("signature failure");
    }

    To2ProveHeaderPayload hrdPayload = Mapper.INSTANCE.readValue(sign1.getPayload(),
        To2ProveHeaderPayload.class);

    SimpleStorage storage = request.getExtra();
    storage.put(OwnerPublicKey.class, ownerPublicKey);
    byte[] original = storage.get(byte[].class);
    Nonce nonceTO2ProveOv = storage.get(Nonce.class);

    if (!hrdPayload.getNonce().equals(nonceTO2ProveOv.getNonce())) {
      throw new InvalidMessageException("NonceTO2ProveOV noes not match");
    }

    HashType hashType = new AlgorithmFinder()
        .getCompatibleHashType(hrdPayload.getHmac().getHashType());

    Hash hash = cs.hash(hashType, original);
    if (!hash.equals(hrdPayload.getHelloHash())) {
      throw new InvalidMessageException("HelloDevice Hash noes not match");
    }

    DeviceCredential cred = getWorker(DeviceCredentialSupplier.class).get();
    Hash hmac = cs.hash(hrdPayload.getHmac().getHashType(), cred.getHmacSecret(),
        hrdPayload.getHeader());
    if (!hmac.equals(hrdPayload.getHmac())) {
      throw new InvalidMessageException("header hmac does not match");
    }

    //if bypass there will be no CoseSign1 to verify
    if (null != storage.get(CoseSign1.class)) {
      CoseSign1 to1d = storage.get(CoseSign1.class);
      if (!cs.verify(to1d, ownerPublicKey)) {
        throw new InvalidMessageException("to1d signature failure");
      }
    }
    To2GetNextEntry reqEntry = new To2GetNextEntry();
    reqEntry.setEntryNum(0);
    response.setMessage(Mapper.INSTANCE.writeValue(reqEntry));

    storage.put(To2ProveHeaderPayload.class, hrdPayload);
    storage.put(OwnerPublicKey.class, ownerPublicKey);

    //store device nonce as to2 done
    To2Done to2Done = new To2Done();
    to2Done.setNonce(sign1.getUnprotectedHeader().getCupNonce());
    storage.put(To2Done.class, to2Done);


  }

  protected void doGetNextEntry(DispatchMessage request, DispatchMessage response)
      throws IOException {

    To2GetNextEntry reqEntry = request.getMessage(To2GetNextEntry.class);
    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = manager.getSession(request.getAuthToken().get());
    OwnershipVoucher voucher = storage.get(OwnershipVoucher.class);
    if (reqEntry.getEntryNum() < 0 || reqEntry.getEntryNum() > voucher.getEntries().size()) {
      throw new InvalidMessageException("invalid numentry " + reqEntry.getEntryNum());
    }

    To2NextEntry nextEntry = new To2NextEntry();
    nextEntry.setEntryNum(reqEntry.getEntryNum());
    nextEntry.setEntry(voucher.getEntries().get(reqEntry.getEntryNum()));
    response.setMessage(Mapper.INSTANCE.writeValue(nextEntry));
  }

  protected void doNextEntry(DispatchMessage request, DispatchMessage response) throws IOException {

    To2NextEntry nextEntry = request.getMessage(To2NextEntry.class);
    if (nextEntry.getEntryNum() < 0) {
      throw new InvalidMessageException("num entry less than zero");
    }
    CoseSign1 entry = nextEntry.getEntry();

    SimpleStorage storage = request.getExtra();

    To2ProveHeaderPayload hdrPayload = storage.get(To2ProveHeaderPayload.class);

    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(hdrPayload.getHeader(), OwnershipVoucherHeader.class);

    HashType hashType =
        new AlgorithmFinder().getCompatibleHashType(hdrPayload.getHmac().getHashType());
    Hash hdrHash = VoucherUtils.getHeaderHash(hashType, header);
    Hash prevHdrHash = null;
    OwnerPublicKey prevOwnerKey = null;
    CryptoService cs = getCryptoService();
    if (nextEntry.getEntryNum() == 0) {
      prevOwnerKey = header.getPublicKey();
      prevHdrHash = VoucherUtils.getEntryHash(hdrPayload.getHmac(),
          hdrPayload.getHeader());
    } else {
      prevOwnerKey = storage.get(OwnerPublicKey.class);
      prevHdrHash = storage.get(Hash.class);
    }

    boolean verified = cs.verify(entry, prevOwnerKey);
    if (!verified) {
      throw new InvalidMessageException("entry signature failure");
    }

    OwnershipVoucherEntryPayload entryPayload =
        Mapper.INSTANCE.readValue(entry.getPayload(), OwnershipVoucherEntryPayload.class);

    if (!entryPayload.getHeaderHash().equals(hdrHash)) {
      throw new InvalidMessageException("entry header hash failure");
    }

    if (!entryPayload.getPreviousHash().equals(prevHdrHash)) {
      throw new InvalidMessageException("entry previous hash failure");
    }

    if (hdrPayload.getNumEntries() == nextEntry.getEntryNum() + 1) {

      doTo2Eat(request, response);

    } else {
      storage.put(OwnerPublicKey.class, entryPayload.getOwnerPublicKey());
      storage.put(Hash.class,
          cs.hash(hashType, Mapper.INSTANCE.writeValue(entry)));

      To2GetNextEntry reqEntry = new To2GetNextEntry();
      reqEntry.setEntryNum(nextEntry.getEntryNum() + 1);
      response.setMessage(Mapper.INSTANCE.writeValue(reqEntry));
    }

  }

  protected void doTo2Eat(DispatchMessage request, DispatchMessage response)
      throws IOException {

    response.setMsgType(MsgType.TO2_PROVE_DEVICE);

    SimpleStorage storage = request.getExtra();

    Nonce setupNonce = Nonce.fromRandomUuid();
    CoseUnprotectedHeader uph = new CoseUnprotectedHeader();
    uph.setEatNonce(setupNonce); //NonceTO2SetupDv
    To2ProveDevicePayload fdoPayload = new To2ProveDevicePayload();
    CryptoService cs = getCryptoService();

    //get hello message
    HelloDevice helloDevice = Mapper.INSTANCE.readValue(storage.get(byte[].class),
        HelloDevice.class);

    OwnerPublicKey ownerPublicKey = storage.get(OwnerPublicKey.class);
    KexMessage kexMessage = cs.getKeyExchangeMessage(helloDevice.getKexSuiteName(),
        KexParty.B,
        ownerPublicKey);

    fdoPayload.setKexB(kexMessage.getMessage());
    EatPayloadBase eat = new EatPayloadBase();

    eat.setFdoClaim(AnyType.fromObject(fdoPayload));

    Nonce deviceNonce = storage.get(To2Done.class).getNonce();//get stored device nonce
    eat.setNonce(deviceNonce);

    To2ProveHeaderPayload hdrPayload = storage.get(To2ProveHeaderPayload.class);

    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(hdrPayload.getHeader(), OwnershipVoucherHeader.class);
    eat.setGuid(header.getGuid());

    KeyResolver resolver = Config.getWorker(DeviceKeySupplier.class).get();
    Certificate[] chain = resolver.getCertificateChain();

    OwnerPublicKey pubKey = getCryptoService().encodeKey(
        new AlgorithmFinder().getPublicKeyType(chain[0].getPublicKey()),
        PublicKeyEncoding.X509,
        chain);
    PrivateKey privateKey = resolver.getPrivateKey(chain[0].getPublicKey());
    try {
      byte[] payload = Mapper.INSTANCE.writeValue(eat);
      CoseSign1 sign1 = getCryptoService().sign(payload, privateKey, pubKey);
      sign1.setUnprotectedHeader(uph);
      response.setMessage(Mapper.INSTANCE.writeValue(sign1));
    } finally {
      cs.destroyKey(privateKey);
    }

    //build device key exchange
    KeyExchangeResult kxResult = cs.getSharedSecret(helloDevice.getKexSuiteName(),
        hdrPayload.getKexA(), kexMessage, null);
    EncryptionState es =
        getCryptoService().getEncryptionState(kxResult, helloDevice.getCipherSuiteType());

    storage.put(EncryptionState.class, es);

    //store setup nonce as to2done2 message
    To2Done2 done2 = new To2Done2();
    done2.setNonce(setupNonce);
    storage.put(To2Done2.class, done2);

  }

  protected void doTo2ProveDevice(DispatchMessage request, DispatchMessage response)
      throws IOException {

    CoseSign1 sign1 = request.getMessage(CoseSign1.class);

    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = manager.getSession(request.getAuthToken().get());
    OwnershipVoucher voucher = storage.get(OwnershipVoucher.class);
    HelloDevice helloDevice = storage.get(HelloDevice.class);

    CertChain certChain = voucher.getCertChain();
    OwnerPublicKey pubKey = null;
    if (certChain != null) {
      pubKey = getCryptoService().encodeKey(
          new AlgorithmFinder().getPublicKeyType(certChain.getChain().get(0).getPublicKey()),
          PublicKeyEncoding.X509,
          certChain.getChain().toArray(Certificate[]::new));
      boolean verified = getCryptoService().verify(sign1, pubKey);
      if (!verified) {
        throw new InvalidMessageException("signature failure");
      }
    } else {
      //we don't have the cert chain so the only way is to verify using sigInfo
      boolean verified = getCryptoService().verify(
          sign1,
          helloDevice.getSigInfo());
      if (!verified) {
        throw new InvalidOwnerSignException();
      }
    }

    EatPayloadBase eatPayload = Mapper.INSTANCE.readValue(sign1.getPayload(), EatPayloadBase.class);
    if (!eatPayload.getGuid().equals(helloDevice.getGuid())) {
      throw new InvalidMessageException("EUID does not match");
    }

    Nonce deviceNonce = storage.get(To2Done.class).getNonce(); // this is sent in 61
    if (!deviceNonce.equals(eatPayload.getNonce())) {
      throw new InvalidMessageException("NonceTO2ProveDv does not match");
    }

    //gets setup nonce
    Nonce setupNonce = sign1.getUnprotectedHeader().getEatNonce();
    //store setup nonce for later
    To2Done2 done2 = new To2Done2();
    done2.setNonce(setupNonce);
    storage.put(To2Done2.class, done2);

    To2SetupDevicePayload setupDevice = new To2SetupDevicePayload();
    setupDevice.setNonce(setupNonce);

    OwnershipVoucherHeader replacedHeader =
        getWorker(VoucherReplacementFunction.class).apply(voucher);
    storage.put(OwnershipVoucherHeader.class, replacedHeader);

    setupDevice.setGuid(replacedHeader.getGuid());
    setupDevice.setRendezvousInfo(replacedHeader.getRendezvousInfo());
    setupDevice.setOwner2Key(replacedHeader.getPublicKey());

    // if ownerkey is RSA restricted type and Kex is ASYMKEX
    // then extract the private key in order to determine the
    // shared secret. Other key exchange types do not require
    // the private key.
    KeyResolver ownerKeyResolver = getWorker(OwnerKeySupplier.class).get();
    PrivateKey originalPrivateKey = null;
    String alias;
    if (helloDevice.getKexSuiteName().equalsIgnoreCase("ASYMKEX2048")) {
      alias = KeyResolver.getAlias(PublicKeyType.RSA2048RESTR, KeySizeType.SIZE_2048);
      originalPrivateKey = ownerKeyResolver.getPrivateKey(alias);
    } else if (helloDevice.getKexSuiteName().equalsIgnoreCase("ASYMKEX3072")) {
      alias = KeyResolver.getAlias(PublicKeyType.RSA2048RESTR, KeySizeType.SIZE_3072);
      originalPrivateKey = ownerKeyResolver.getPrivateKey(alias);
    }

    CryptoService cs = getCryptoService();

    To2ProveHeaderPayload hdrPayload = storage.get(To2ProveHeaderPayload.class);

    OwnershipVoucherHeader oldHeader = Mapper.INSTANCE.readValue(hdrPayload.getHeader(),
        OwnershipVoucherHeader.class);
    PublicKey owner1 = cs.decodeKey(oldHeader.getPublicKey());
    PublicKey owner2 = cs.decodeKey(setupDevice.getOwner2Key());
    KeyResolver resolver = null;
    if (owner2.equals(owner1)) {
      resolver = getWorker(OwnerKeySupplier.class).get();
      setupDevice.setOwner2Key(VoucherUtils.getLastOwner(voucher));
    } else {
      resolver = getWorker(ReplacementKeySupplier.class).get();
    }

    PrivateKey privateKey = resolver.getPrivateKey(
        KeyResolver.getAlias(setupDevice.getOwner2Key().getType(),
            new AlgorithmFinder().getKeySizeType(cs.decodeKey(setupDevice.getOwner2Key()))));
    sign1 = null;
    try {
      byte[] payload = Mapper.INSTANCE.writeValue(setupDevice);
      sign1 = cs.sign(payload, privateKey, setupDevice.getOwner2Key());
    } finally {
      cs.destroyKey(privateKey);
    }

    To2ProveDevicePayload fdoPayload =
        eatPayload.getFdoClaim().covertValue(To2ProveDevicePayload.class);

    KexMessage kexMessage = storage.get(KexMessage.class);
    // determine the sharedsecret for KEX
    KeyExchangeResult kxResult = cs
        .getSharedSecret(helloDevice.getKexSuiteName(),
            fdoPayload.getKexB(), kexMessage, originalPrivateKey);

    EncryptionState es =
        getCryptoService().getEncryptionState(kxResult, helloDevice.getCipherSuiteType());

    storage.put(EncryptionState.class, es);

    response.setMessage(
        cs.encrypt(Mapper.INSTANCE.writeValue(sign1), es));

    manager.updateSession(request.getAuthToken().get(), storage);

  }

  protected void doSetupDevice(DispatchMessage request, DispatchMessage response)
      throws IOException {

    SimpleStorage storage = request.getExtra();
    EncryptionState es = storage.get(EncryptionState.class);

    byte[] cipherText = getCryptoService().decrypt(request.getMessage(), es);

    CoseSign1 sign1 = Mapper.INSTANCE.readValue(cipherText, CoseSign1.class);
    To2SetupDevicePayload payload =
        Mapper.INSTANCE.readValue(sign1.getPayload(), To2SetupDevicePayload.class);

    CryptoService cs = getCryptoService();
    OwnerPublicKey ownerPublicKey = payload.getOwner2Key();
    boolean verify = cs.verify(sign1, ownerPublicKey);
    if (!verify) {
      throw new InvalidMessageException("signature failure");
    }

    To2ProveHeaderPayload hdrPayload = storage.get(To2ProveHeaderPayload.class);
    OwnershipVoucherHeader oldHeader =
        Mapper.INSTANCE.readValue(hdrPayload.getHeader(), OwnershipVoucherHeader.class);
    OwnershipVoucherHeader newHeader = new OwnershipVoucherHeader();
    newHeader.setVersion(oldHeader.getVersion());
    newHeader.setDeviceInfo(oldHeader.getDeviceInfo());
    newHeader.setCertHash(oldHeader.getCertHash());
    newHeader.setGuid(payload.getGuid());
    newHeader.setPublicKey(payload.getOwner2Key());
    newHeader.setRendezvousInfo(payload.getRendezvousInfo());

    Hash oldMac = hdrPayload.getHmac();
    DeviceCredential cred = getWorker(DeviceCredentialSupplier.class).get();
    Hash newMac = cs.hash(oldMac.getHashType(), cred.getHmacSecret(),
        Mapper.INSTANCE.writeValue(newHeader));

    //check cred resuse
    boolean credReuse = oldHeader.getGuid().equals(newHeader.getGuid());

    if (credReuse) {
      byte[] rv1 = Mapper.INSTANCE.writeValue(oldHeader.getRendezvousInfo());
      byte[] rv2 = Mapper.INSTANCE.writeValue(newHeader.getRendezvousInfo());
      if (!Arrays.equals(rv1, rv2)) {
        credReuse = false;
      }
    }

    if (credReuse) {

      OwnerPublicKey owner1 = storage.get(OwnerPublicKey.class);
      OwnerPublicKey owner2 = payload.getOwner2Key();
      if (!cs.decodeKey(owner1).equals(cs.decodeKey(owner2))) {
        credReuse = false;
      }
    }

    if (credReuse) {
      newMac = null;
      getWorker(CredReuseFunction.class).apply(true);
    } else {
      cred.setGuid(newHeader.getGuid());
      cred.setRvInfo(newHeader.getRendezvousInfo());
    }

    To2DeviceInfoReady devInfoReady = new To2DeviceInfoReady();
    devInfoReady.setHmac(newMac);

    Integer maxSvi = getWorker(MaxServiceInfoSupplier.class).get();
    devInfoReady.setMaxMessageSize(maxSvi);
    if (maxSvi == null) {
      logger.info("max service info size is null (default)");
    } else {
      logger.info("max service info size is " + maxSvi);
    }

    cipherText = Mapper.INSTANCE.writeValue(devInfoReady);
    response.setMessage(cs.encrypt(cipherText, es));

    if (devInfoReady.getMaxMessageSize() == null) {
      devInfoReady.setMaxMessageSize(BufferUtils.getServiceInfoMtuSize());
    }
    storage.put(To2DeviceInfoReady.class, devInfoReady);

  }

  protected void doDeviceInfoReady(DispatchMessage request, DispatchMessage response)
      throws IOException {

    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = manager.getSession(request.getAuthToken().get());
    EncryptionState es = storage.get(EncryptionState.class);

    byte[] cipherText = getCryptoService().decrypt(request.getMessage(), es);

    To2DeviceInfoReady devInfoReady = Mapper.INSTANCE.readValue(cipherText,
        To2DeviceInfoReady.class);

    if (devInfoReady.getMaxMessageSize() == null) {
      devInfoReady.setMaxMessageSize(BufferUtils.getServiceInfoMtuSize());
    }

    storage.put(To2DeviceInfoReady.class, devInfoReady);

    To2OwnerInfoReady ownerInfoReady = new To2OwnerInfoReady();

    ownerInfoReady.setMaxMessageSize(getWorker(OwnerInfoSizeSupplier.class).get());

    cipherText = Mapper.INSTANCE.writeValue(ownerInfoReady);
    response.setMessage(getCryptoService().encrypt(cipherText, es));

    if (ownerInfoReady.getMaxMessageSize() == null) {
      ownerInfoReady.setMaxMessageSize(BufferUtils.getServiceInfoMtuSize());
    }

    storage.put(To2OwnerInfoReady.class, ownerInfoReady);

    ServiceInfoDocument document = getWorker(ServiceInfoDocumentSupplier.class).get();
    storage.put(ServiceInfoDocument.class, document);

    ServiceInfoGlobalState globalState = new ServiceInfoGlobalState();
    HelloDevice helloDevice = storage.get(HelloDevice.class);
    List<Object> workers = getWorkers();
    ServiceInfoModuleList moduleList = new ServiceInfoModuleList();
    for (Object worker : workers) {
      //build the initial state for all modules
      if (worker instanceof ServiceInfoModule) {
        ServiceInfoModule module = (ServiceInfoModule) worker;
        ServiceInfoModuleState state = new ServiceInfoModuleState();
        state.setName(module.getName());
        state.setGuid(helloDevice.getGuid());
        state.setExtra(AnyType.fromObject(new NullValue()));
        state.setMtu(Math.min(devInfoReady.getMaxMessageSize(),
            ownerInfoReady.getMaxMessageSize()));
        state.setDocument(document);
        state.setGlobalState(globalState);
        module.prepare(state);
        moduleList.add(state);
      }
    }
    storage.put(ServiceInfoModuleList.class, moduleList);
    storage.put(ServiceInfoGlobalState.class, globalState);

    OwnerServiceInfo lastInfo = new OwnerServiceInfo();
    lastInfo.setServiceInfo(new ServiceInfo());
    storage.put(OwnerServiceInfo.class, lastInfo);

    if (devInfoReady.getHmac() != null) {
      storage.put(Hash.class, devInfoReady.getHmac());
    } else {
      storage.put(Hash.class, new Hash());
    }

    manager.updateSession(request.getAuthToken().get(), storage);
  }

  protected void doOwnerInfoReady(DispatchMessage request, DispatchMessage response)
      throws IOException {

    SimpleStorage storage = request.getExtra();
    EncryptionState es = storage.get(EncryptionState.class);

    byte[] cipherText = getCryptoService().decrypt(request.getMessage(), es);

    To2OwnerInfoReady ownerInfoReady = Mapper.INSTANCE.readValue(cipherText,
        To2OwnerInfoReady.class);

    if (ownerInfoReady.getMaxMessageSize() == null) {
      ownerInfoReady.setMaxMessageSize(BufferUtils.getServiceInfoMtuSize());
    }
    logger.info("Received maxDeviceServiceInfoSz: " + ownerInfoReady.getMaxMessageSize());

    storage.put(To2OwnerInfoReady.class, ownerInfoReady);

    DeviceServiceInfo devInfo = new DeviceServiceInfo();
    devInfo.setServiceInfo(new ServiceInfo());

    To2ProveHeaderPayload ownerPayload = storage.get(To2ProveHeaderPayload.class);
    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(ownerPayload.getHeader(), OwnershipVoucherHeader.class);


    List<Object> workers = getWorkers();
    ServiceInfoModuleList moduleList = new ServiceInfoModuleList();
    for (Object worker : workers) {
      //build the initial state for all modules
      if (worker instanceof ServiceInfoModule) {
        ServiceInfoModule module = (ServiceInfoModule) worker;
        ServiceInfoModuleState state = new ServiceInfoModuleState();
        state.setName(module.getName());
        state.setGuid(header.getGuid());
        state.setExtra(AnyType.fromObject(new NullValue()));
        state.setMtu(ownerInfoReady.getMaxMessageSize());
        module.prepare(state);
        if (state.getName().equals(DevMod.NAME)) {
          module.send(state, new
              StandardServiceInfoSendFunction(state.getMtu(),
              devInfo.getServiceInfo()));
          devInfo.setMore(state.isMore());
        }
        moduleList.add(state);
      }
    }

    storage.put(ServiceInfoModuleList.class, moduleList);

    cipherText = Mapper.INSTANCE.writeValue(devInfo);
    response.setMessage(getCryptoService().encrypt(cipherText, es));
  }

  protected void doDeviceInfo(DispatchMessage request, DispatchMessage response)
      throws IOException {

    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = manager.getSession(request.getAuthToken().get());
    EncryptionState es = storage.get(EncryptionState.class);

    byte[] cipherText = getCryptoService().decrypt(request.getMessage(), es);

    DeviceServiceInfo devInfo = Mapper.INSTANCE.readValue(cipherText,
        DeviceServiceInfo.class);

    ServiceInfoGlobalState globalState = storage.get(ServiceInfoGlobalState.class);
    ServiceInfoDocument document = storage.get(ServiceInfoDocument.class);
    ServiceInfoModuleList moduleList = storage.get(ServiceInfoModuleList.class);
    for (ServiceInfoModuleState state : moduleList) {

      state.setGlobalState(globalState);
      state.setDocument(document);
      ServiceInfoModule module = getModule(state.getName());
      if (devInfo.getServiceInfo().size() == 0) {
        module.keepAlive();
      }
      for (ServiceInfoKeyValuePair pair : devInfo.getServiceInfo()) {
        module.receive(state, pair);
      }
    }

    ServiceInfo info = new ServiceInfo();
    OwnerServiceInfo ownerInfo = new OwnerServiceInfo();
    ownerInfo.setServiceInfo(info);
    To2DeviceInfoReady devInfoReady = storage.get(To2DeviceInfoReady.class);
    ServiceInfoSendFunction sendFunction =
        new StandardServiceInfoSendFunction(devInfoReady.getMaxMessageSize(), info);

    if (devInfo.isMore()) {
      ownerInfo.setDone(false);
      ownerInfo.setMore(false);
      ownerInfo.setServiceInfo(new ServiceInfo());
    } else {
      ownerInfo.setDone(true);
      for (ServiceInfoModuleState state : moduleList) {
        ServiceInfoModule module = getModule(state.getName());
        module.send(state, sendFunction);
        if (state.isMore()) {
          ownerInfo.setMore(true);
          ownerInfo.setDone(false);
          break;
        }
        if (!state.isDone()) {
          ownerInfo.setDone(false); // if any
        }
      }
    }

    cipherText = Mapper.INSTANCE.writeValue(ownerInfo);
    response.setMessage(getCryptoService().encrypt(cipherText, es));

    manager.updateSession(request.getAuthToken().get(), storage);
  }

  protected void doOwnerInfo(DispatchMessage request, DispatchMessage response)
      throws IOException {

    SimpleStorage storage = request.getExtra();
    EncryptionState es = storage.get(EncryptionState.class);

    byte[] cipherText = getCryptoService().decrypt(request.getMessage(), es);

    OwnerServiceInfo ownerInfo = Mapper.INSTANCE.readValue(cipherText,
        OwnerServiceInfo.class);

    ServiceInfoModuleList moduleList = storage.get(ServiceInfoModuleList.class);

    for (ServiceInfoModuleState state : moduleList) {
      ServiceInfoModule module = getModule(state.getName());
      if (ownerInfo.getServiceInfo().size() == 0) {
        module.keepAlive();
      }
      for (ServiceInfoKeyValuePair pair : ownerInfo.getServiceInfo()) {
        module.receive(state, pair);
      }
    }

    ServiceInfo info = new ServiceInfo();
    DeviceServiceInfo devInfo = new DeviceServiceInfo();
    devInfo.setServiceInfo(info);
    To2OwnerInfoReady ownerInfoReady = storage.get(To2OwnerInfoReady.class);
    ServiceInfoSendFunction sendFunction =
        new StandardServiceInfoSendFunction(ownerInfoReady.getMaxMessageSize(), info);

    if (ownerInfo.isMore()) {
      devInfo.setMore(false);
      devInfo.setServiceInfo(new ServiceInfo());
    } else {
      for (ServiceInfoModuleState state : moduleList) {
        ServiceInfoModule module = getModule(state.getName());
        module.send(state, sendFunction);
        if (state.isMore()) {
          devInfo.setMore(true);
          break;
        }
      }
    }

    if (ownerInfo.isDone() && devInfo.getServiceInfo().isEmpty()) {
      To2Done done = storage.get(To2Done.class);
      response.setMsgType(MsgType.TO2_DONE);
      cipherText = Mapper.INSTANCE.writeValue(done);
    } else {
      cipherText = Mapper.INSTANCE.writeValue(devInfo);
    }
    response.setMessage(getCryptoService().encrypt(cipherText, es));

  }

  protected void doTo2Done(DispatchMessage request, DispatchMessage response)
      throws IOException {
    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = manager.getSession(request.getAuthToken().get());
    EncryptionState es = storage.get(EncryptionState.class);

    byte[] cipherText = getCryptoService().decrypt(request.getMessage(), es);

    To2Done done = Mapper.INSTANCE.readValue(cipherText,
        To2Done.class);

    Nonce deviceNonce = storage.get(To2Done.class).getNonce();
    if (!deviceNonce.equals(done.getNonce())) {
      throw new InvalidMessageException("NonceTO2ProveDv noes not match");
    }

    OwnershipVoucherHeader replaceHeader = storage.get(OwnershipVoucherHeader.class);
    OwnershipVoucher voucher = storage.get(OwnershipVoucher.class);

    OwnershipVoucher replaceVoucher = new OwnershipVoucher();
    Hash hmac = storage.get(Hash.class);
    if (hmac.getHashValue() != null) {
      replaceVoucher.setCertChain(voucher.getCertChain());
      replaceVoucher.setHeader(Mapper.INSTANCE.writeValue(replaceHeader));
      replaceVoucher.setEntries(new OwnershipVoucherEntries());
      replaceVoucher.setVersion(voucher.getVersion());
      replaceVoucher.setHmac(hmac);
    }
    getWorker(ReplacementVoucherStorageFunction.class).apply(voucher, replaceVoucher);

    To2Done2 done2 = storage.get(To2Done2.class);
    cipherText = Mapper.INSTANCE.writeValue(done2);
    response.setMessage(getCryptoService().encrypt(cipherText, es));
    manager.expireSession(request.getAuthToken().get());


  }

  protected void doTo2Done2(DispatchMessage request, DispatchMessage response)
      throws IOException {

    SimpleStorage storage = request.getExtra();
    EncryptionState es = storage.get(EncryptionState.class);

    byte[] cipherText = getCryptoService().decrypt(request.getMessage(), es);

    To2Done2 done2 = Mapper.INSTANCE.readValue(cipherText,
        To2Done2.class);

    Nonce setupNonce = storage.get(To2Done2.class).getNonce();
    if (!setupNonce.equals(done2.getNonce())) {
      throw new InvalidMessageException("NonceTO2SetupDv noes not match");
    }
    logger.info("TO2 completed successfully.");
    logger.info("===== FDO TO2 SUCCESS =====");
  }

  protected void doError(DispatchMessage request, DispatchMessage response) throws IOException {

    for (Object worker : getWorkers()) {
      if (worker instanceof SessionManager) {
        if (request.getAuthToken().isPresent()) {
          SessionManager manager = (SessionManager) worker;
          manager.expireSession(request.getAuthToken().get());
        }
      }
    }

  }


  @Override
  public Optional<DispatchMessage> dispatch(DispatchMessage request) throws IOException {
    DispatchMessage response = new DispatchMessage();
    response.setMsgType(MsgType.ERROR);
    if (request.getAuthToken().isPresent()) {
      response.setAuthToken(request.getAuthToken().get());
    }
    response.setProtocolVersion(request.getProtocolVersion());

    switch (request.getMsgType()) {
      case DI_APP_START:
        response.setAuthToken(createSessionId());
        response.setMsgType(MsgType.DI_SET_CREDENTIALS);
        doAppStart(request, response);
        break;
      case DI_SET_CREDENTIALS:
        response.setMsgType(MsgType.DI_SET_HMAC);
        doSetCredentials(request, response);
        break;
      case DI_SET_HMAC:
        response.setMsgType(MsgType.DI_DONE);
        doSetHmac(request, response);
        break;
      case DI_DONE:
        doDiDone(request, response);
        return Optional.empty();
      case TO0_HELLO:
        response.setMsgType(MsgType.TO0_HELLO_ACK);
        doTo0Hello(request, response);
        break;
      case TO0_HELLO_ACK:
        response.setMsgType(MsgType.TO0_OWNER_SIGN);
        doTo0HelloAck(request, response);
        break;
      case TO0_OWNER_SIGN:
        response.setMsgType(MsgType.TO0_ACCEPT_OWNER);
        doTo0OwnerSign(request, response);
        break;
      case TO0_ACCEPT_OWNER:
        doTo0AcceptOwner(request, response);
        return Optional.empty();
      case TO1_HELLO_RV:
        response.setMsgType(MsgType.TO1_HELLO_RV_ACK);
        doTo1Hello(request, response);
        break;
      case TO1_HELLO_RV_ACK:
        response.setMsgType(MsgType.TO1_PROVE_TO_RV);
        doTo1HelloAck(request, response);
        break;
      case TO1_PROVE_TO_RV:
        response.setMsgType(MsgType.TO1_RV_REDIRECT);
        doTo1ProveRv(request, response);
        break;
      case TO1_RV_REDIRECT:
        doTo1Redirect(request, response);
        return Optional.empty();
      case TO2_HELLO_DEVICE:
        response.setAuthToken(createSessionId());
        response.setMsgType(MsgType.TO2_PROVE_OV_HDR);
        doHelloDevice(request, response);
        break;
      case TO2_PROVE_OV_HDR:
        response.setMsgType(MsgType.TO2_GET_OV_NEXT_ENTRY);
        doProveOwnerHeader(request, response);
        break;
      case TO2_GET_OV_NEXT_ENTRY:
        response.setMsgType(MsgType.TO2_OV_NEXT_ENTRY);
        doGetNextEntry(request, response);
        break;
      case TO2_OV_NEXT_ENTRY:
        response.setMsgType(MsgType.TO2_GET_OV_NEXT_ENTRY);
        doNextEntry(request, response);
        break;
      case TO2_PROVE_DEVICE:
        response.setMsgType(MsgType.TO2_SETUP_DEVICE);
        doTo2ProveDevice(request, response);
        break;
      case TO2_SETUP_DEVICE:
        response.setMsgType(MsgType.TO2_DEVICE_SERVICE_INFO_READY);
        doSetupDevice(request, response);
        break;
      case TO2_DEVICE_SERVICE_INFO_READY:
        response.setMsgType(MsgType.TO2_OWNER_SERVICE_INFO_READY);
        doDeviceInfoReady(request, response);
        break;
      case TO2_OWNER_SERVICE_INFO_READY:
        response.setMsgType(MsgType.TO2_DEVICE_SERVICE_INFO);
        doOwnerInfoReady(request, response);
        break;
      case TO2_DEVICE_SERVICE_INFO:
        response.setMsgType(MsgType.TO2_OWNER_SERVICE_INFO);
        doDeviceInfo(request, response);
        break;
      case TO2_OWNER_SERVICE_INFO:
        response.setMsgType(MsgType.TO2_DEVICE_SERVICE_INFO);
        doOwnerInfo(request, response);
        break;
      case TO2_DONE:
        doTo2Done(request, response);
        response.setMsgType(MsgType.TO2_DONE2);
        break;
      case TO2_DONE2:
        doTo2Done2(request, response);
        return Optional.empty();
      case ERROR:
        doError(request, response);
        return Optional.empty();
      default:
        break;
    }

    return Optional.of(response);
  }
}
