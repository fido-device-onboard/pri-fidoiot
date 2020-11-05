// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.epid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;

public class EpidSignatureVerifier {

  /**
   * Verifies EPID signature. Returns boolean 'true' if the signature verification is successful.
   * Returns boolean 'false' otherwise.
   * 
   * @param signatureComposite {@link Composite} that represents the COSE signature object
   * @param sigInfoAComposite {@link Composite} that represents the SigInfo object
   * @return boolean value true/false
   */
  public static boolean verify(Composite signatureComposite, Composite sigInfoAComposite) {
    try {
      if (null == signatureComposite || null == sigInfoAComposite) {
        throw new RuntimeException();
      }
      int sgType = sigInfoAComposite.getAsNumber(Const.FIRST_KEY).intValue();
      byte[] groupId = sigInfoAComposite.getAsBytes(Const.SECOND_KEY);
      byte[] msg = createEpidSignatureBodyMessage(signatureComposite, groupId, sgType);
      String url = null;
      String path;
      switch (sgType) {
        case Const.SG_EPIDv10:
        case Const.SG_EPIDv11:
          path = String.join(Const.URL_PATH_SEPARATOR, Arrays.asList(Const.EPID_PROTOCOL_VERSION_V1,
                  Const.EPID_11, Const.EPID_PROOF_URI_PATH));
          url = new URL(EpidUtils.getEpidOnlineUrl().toString()).toURI()
              .resolve(Const.URL_PATH_SEPARATOR + path).toString();
          break;
        case Const.SG_EPIDv20:
          path = String.join(Const.URL_PATH_SEPARATOR, Arrays.asList(Const.EPID_PROTOCOL_VERSION_V1,
                  Const.EPID_20, Const.EPID_PROOF_URI_PATH));
          url = new URL(EpidUtils.getEpidOnlineUrl().toString()).toURI()
              .resolve(Const.URL_PATH_SEPARATOR + path).toString();
          break;
        default:
          throw new IOException("Invalid sgType");
      }
      byte[] response = EpidHttpClient.doPost(url, msg);
      if (null != response && response.length == 0) {
        return true;
      } else {
        return false;
      }
    } catch (IOException | URISyntaxException ex) {
      return false;
    }
  }

  private static byte[] createEpidSignatureBodyMessage(Composite signatureComposite, byte[] groupId,
      int sgType) throws IOException {
    byte[] signature = signatureComposite.getAsBytes(Const.COSE_SIGN1_SIGNATURE);
    byte[] signedPayload = createEpidPayload(signatureComposite, sgType);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(groupId);
    baos.write(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
        .putShort((short) signedPayload.length).array());
    baos.write(signedPayload);
    baos.write(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short) 0).array());
    baos.write(signature);
    return baos.toByteArray();
  }

  // Generate the signed Payload depnding on the sgType.
  private static byte[] createEpidPayload(Composite signatureBody, int sgType) throws IOException {
    Composite uph = signatureBody.getAsComposite(Const.COSE_SIGN1_UNPROTECTED);
    byte[] payloadAsBytes = signatureBody.getAsBytes(Const.COSE_SIGN1_PAYLOAD);
    Composite payload = Composite.fromObject(payloadAsBytes);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    switch (sgType) {
      case Const.SG_EPIDv10:
        baos.write((byte) uph.getAsBytes(Const.EAT_MAROE_PREFIX).length);
        baos.write(uph.getAsBytes(Const.EAT_MAROE_PREFIX));
        baos.write(payload.getAsBytes(Const.EAT_NONCE));
        baos.write(payloadAsBytes);
        break;
      case Const.SG_EPIDv11:
        baos.write(ByteBuffer.allocate(48).put(4, (byte) 0x48).put(8, (byte) 0x08).array());
        baos.write(uph.getAsBytes(Const.EAT_MAROE_PREFIX));
        baos.write(ByteBuffer.allocate(16).array());
        baos.write(payload.getAsBytes(Const.EAT_NONCE));
        baos.write(ByteBuffer.allocate(16).array());
        baos.write(payloadAsBytes);
        break;
      case Const.SG_EPIDv20:
        baos.write(payloadAsBytes);
        break;
      default:
        return null;
    }
    return baos.toByteArray();
  }
}
