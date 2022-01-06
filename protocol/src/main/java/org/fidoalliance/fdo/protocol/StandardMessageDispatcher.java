package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.dispatch.CertSignatureFunction;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialConsumer;
import org.fidoalliance.fdo.protocol.dispatch.HmacFunction;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.MessageDispatcher;
import org.fidoalliance.fdo.protocol.dispatch.RendezvousInfoSupplier;
import org.fidoalliance.fdo.protocol.dispatch.SessionManager;
import org.fidoalliance.fdo.protocol.dispatch.VoucherStorageFunction;
import org.fidoalliance.fdo.protocol.message.CertChain;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;
import org.fidoalliance.fdo.protocol.message.DiDone;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherEntries;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.AppStart;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.SetCredentials;
import org.fidoalliance.fdo.protocol.message.SetHmac;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;

public class StandardMessageDispatcher implements MessageDispatcher {

  protected StandardCryptoService getCryptoService() {
    return Config.getWorker(StandardCryptoService.class);
  }

  protected <T> T getWorker(Class<T> t) {
    return Config.getWorker(t);
  }

  protected String createSessionId() {
    return Hex.encodeHexString(getCryptoService().getRandomBytes(Long.BYTES * 2));
  }

  protected SimpleStorage createVoucher(AnyType mfgInfo, ProtocolVersion version)
      throws IOException {

    final OwnershipVoucher voucher = new OwnershipVoucher();
    final OwnershipVoucherHeader header = new OwnershipVoucherHeader();
    final CryptoService cs = getCryptoService();
    final ManufacturingInfo diInfo = mfgInfo.unwrap(ManufacturingInfo.class);
    final Guid guid = Guid.fromRandomUUID();

    header.setVersion(version);
    header.setGuid(guid);
    header.setDeviceInfo(diInfo.getDeviceInfo());

    //todo: handle Ondie ECDSA and EPID

    final AnyType certInfo = diInfo.getCertInfo();
    if (certInfo != null) {
      try {

        Certificate[] chain = getWorker(CertSignatureFunction.class).apply(diInfo);

        int totalBytes = 0;
        for (Certificate cert : chain) {
          totalBytes += cert.getEncoded().length;
        }

        final ByteBuffer buff = ByteBuffer.allocate(totalBytes);
        for (int i = 0; i < chain.length; i++) {
          buff.put(chain[i].getEncoded());
        }
        buff.flip();
        byte[] data = BufferUtils.unwrap(buff);

        buff.clear();

        List<Certificate> certList = new ArrayList<>(Arrays.asList(chain));
        voucher.setCertChain(CertChain.fromList(certList));

        header.setPublicKey(cs.encodeKey(diInfo.getKeyType(),
            diInfo.getKeyEnc(), chain));

        //todo: verifychain

        //set the cert hash
        header.setCertHash(
            cs.hash(new AlgorithmFinder().getCompatibleHashType(
                    certList.get(0).getPublicKey()),
                data));

      } catch (CertificateEncodingException e) {
        throw new IOException(e);
      }
    } else {
      // no certInfo provided so assume epid and not cert chain or hash
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
    final AnyType headerTag = AnyType.fromObject(header);
    headerTag.wrap();
    voucher.setHeader(headerTag);

    SimpleStorage storage = new SimpleStorage();
    storage.put(OwnershipVoucher.class, voucher);
    storage.put(ManufacturingInfo.class, diInfo);
    return storage;
  }

  protected void doAppStart(DispatchMessage request, DispatchMessage response) throws IOException {
    AppStart appStart = request.getMessage(AppStart.class);
    AnyType mfgInfo = appStart.getManufacturingInfo();
    SimpleStorage storage = createVoucher(mfgInfo, request.getProtocolVersion());

    SessionManager manager = getWorker(SessionManager.class);

    OwnershipVoucher voucher = storage.get(OwnershipVoucher.class);

    AnyType headerTag = voucher.getHeader();
    SetCredentials setCredentials = new SetCredentials();
    setCredentials.setVoucherHeader(headerTag);
    response.setMessage(AnyType.fromObject(setCredentials));

    manager.saveSession(response.getAuthToken().get(),
        storage);

  }

  protected void doSetCredentials(DispatchMessage request, DispatchMessage response)
      throws IOException {

    CryptoService cs = getCryptoService();
    SetCredentials setCredentials = request.getMessage(SetCredentials.class);
    AnyType headerTag = setCredentials.getVoucherHeader();
    OwnershipVoucherHeader header = headerTag.unwrap(OwnershipVoucherHeader.class);

    DeviceCredential credential = new DeviceCredential();

    HmacFunction hmacFunction = getWorker(HmacFunction.class);
    Hash hmac = hmacFunction.apply(credential, headerTag);

    SetHmac setHmac = new SetHmac();
    setHmac.setHmac(hmac);
    response.setMessage(AnyType.fromObject(setHmac));

    credential.setActive(true);
    credential.setGuid(header.getGuid());
    credential.setDeviceInfo(header.getDeviceInfo());
    credential.setProtVer(response.getProtocolVersion());
    credential.setRvInfo(header.getRendezvousInfo());

    PublicKey publicKey = cs.decodeKey(header.getPublicKey());
    HashType hashType = new AlgorithmFinder().getCompatibleHashType(hmac.getHashType());
    credential.setPubKeyHash(cs.hash(hashType, publicKey.getEncoded()));

    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = new SimpleStorage();
    storage.put(DeviceCredential.class, credential);
    manager.saveSession(request.getAuthToken().get(), storage);

  }

  public void doSetHmac(DispatchMessage request, DispatchMessage response) throws IOException {

    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = manager.getSession(request.getAuthToken().get());
    OwnershipVoucher voucher = storage.get(OwnershipVoucher.class);
    ManufacturingInfo info = storage.get(ManufacturingInfo.class);
    SetHmac setHmac = request.getMessage(SetHmac.class);
    voucher.setHmac(setHmac.getHmac());

    VoucherStorageFunction storageFunction = getWorker(VoucherStorageFunction.class);
    storageFunction.apply(info.getSerialNumber(),voucher);

    //save the voucher
    response.setMessage(AnyType.fromObject(new DiDone()));
  }

  public void doDiDone(DispatchMessage request, DispatchMessage response) throws IOException {

    SessionManager manager = getWorker(SessionManager.class);
    SimpleStorage storage = manager.getSession(request.getAuthToken().get());
    DeviceCredential credential = storage.get(DeviceCredential.class);
    DeviceCredentialConsumer consumer = getWorker(DeviceCredentialConsumer.class);
    consumer.accept(credential);
    manager.expireSession(request.getAuthToken().get());
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
      case ERROR:
        return Optional.empty(); //never respond to error
      default:
        //response.setBody();
        break;
    }

    return Optional.of(response);
  }
}
