package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.function.Failable;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialConsumer;
import org.fidoalliance.fdo.protocol.dispatch.HmacFunction;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.message.*;
import org.h2.tools.Server;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class TestDatabaseServer implements DatabaseServer, Closeable {

    private Server webServer;
    private Server tcpServer;

    @Override
    public void start() throws IOException {
        try {
            String[] tcpArgs = {"-ifNotExists"};
            tcpServer = Server.createTcpServer(tcpArgs).start();

            String[] webArgs = {"-webPort", "8082"};
            if (webArgs != null) {
                webServer = Server.createWebServer(webArgs).start();
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (webServer != null && webServer.isRunning(true)) {
            webServer.stop();
            webServer = null;
        }
        if (tcpServer != null && tcpServer.isRunning(true)) {
            tcpServer.stop();
            tcpServer = null;
        }
    }
}


class TestCredentialConsumer implements DeviceCredentialConsumer {

    @Override
    public void accept(DeviceCredential deviceCredential) {
        try {
            File credFile = new File(Path.of(Config.getPath(), "credentials.bin").toString());
            byte[] data = Mapper.INSTANCE.writeValue(deviceCredential);
            try (FileOutputStream out = new FileOutputStream(credFile)) {
                out.write(data);
            }
        } catch (FileNotFoundException e) {
            throw Failable.rethrow(e);
        } catch (IOException e) {
            throw Failable.rethrow(e);
        }
    }
}

class TestHmacFunction implements HmacFunction {

    @Override
    public Hash apply(DeviceCredential credential, byte[] headerTag) throws IOException {

        HashType hmacType = HashType.HMAC_SHA256;
        OwnershipVoucherHeader header =
                    Mapper.INSTANCE.readValue(headerTag, OwnershipVoucherHeader.class);
        DiSample.setGuid(header.getGuid().toString());
        CryptoService cs = Config.getWorker(CryptoService.class);
        credential.setHmacSecret(cs.createHmacKey(hmacType));
        return cs.hash(hmacType, credential.getHmacSecret(), headerTag);
    }
}

public class DiSample extends HttpClient {

    private static String guid;

    public static void setGuid(String guid) {
      DiSample.guid = guid;
    }

    public static String getGuid() {
        return DiSample.guid;
    }

    @Override
    protected void generateHello() throws IOException {
        PublicKeyType keyType = PublicKeyType.SECP256R1;
        if (!keyType.equals(PublicKeyType.SECP384R1) && !keyType.equals(PublicKeyType.SECP256R1)) {
            throw new InternalServerErrorException(new IllegalArgumentException("invalid key type"));
        }

        KeySizeType keySize = KeySizeType.SIZE_384;
        if (keyType.equals(PublicKeyType.SECP256R1)) {
            keySize = KeySizeType.SIZE_256;
        }

        List<HttpInstruction> httpInst = new ArrayList<>();
        HttpInstruction instruction = new HttpInstruction();
        instruction.setAddress("http://localhost:8080");
        httpInst.add(instruction);

        setInstructions(httpInst);

        String serialNo = "0";

        logger.info("Device Serial No:" + serialNo);

        byte[] csr = generateCsr(keyType, keySize);

        ManufacturingInfo mfgInfo = new ManufacturingInfo();
        mfgInfo.setKeyType(keyType);
        mfgInfo.setKeyEnc(PublicKeyEncoding.X509);
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


            final KeyResolver keyResolver = Config.getWorker(OwnerKeySupplier.class).get();

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

}
