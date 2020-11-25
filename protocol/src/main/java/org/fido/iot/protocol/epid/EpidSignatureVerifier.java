// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.epid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;


public class EpidSignatureVerifier {

  public enum Result {
    VERIFIED,
    MALFORMED_REQUEST,
    INVALID_SIGNATURE,
    OUTDATED_SIGRL,
    UNKNOWN_ERROR
  }

  /**
   * Verifies EPID signature. Returns result of verification via enum Result.
   * 
   * @param signatureComposite {@link Composite} that represents the COSE signature object
   * @param sigInfoAComposite {@link Composite} that represents the SigInfo object
   * @return VerificationResult result of verification
   */
  public static Result verify(Composite signatureComposite, Composite sigInfoAComposite) {
    try {
      if (null == signatureComposite || null == sigInfoAComposite) {
        throw new RuntimeException();
      }
      int sgType = sigInfoAComposite.getAsNumber(Const.FIRST_KEY).intValue();
      byte[] groupId = sigInfoAComposite.getAsBytes(Const.SECOND_KEY);
      String msg = createEpidSignatureBodyMessage(signatureComposite, groupId, sgType);
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
        default:
          throw new IOException("Invalid sgType");
      }

      int response = EpidHttpClient.doPost(url, msg);
      switch (response) {
        case 200:
          return Result.VERIFIED;
        case 400:
          return Result.MALFORMED_REQUEST;
        case 403:
          return Result.INVALID_SIGNATURE;
        case 417:
          return Result.OUTDATED_SIGRL;
        default:
          return Result.UNKNOWN_ERROR;
      }
    } catch (IOException | URISyntaxException ex) {
      return Result.UNKNOWN_ERROR;
    }
  }

  private static String createEpidSignatureBodyMessage(Composite signatureComposite, byte[] groupId,
      int sgType) throws IOException {
    byte[] signature = signatureComposite.getAsBytes(Const.COSE_SIGN1_SIGNATURE);
    byte[] signedPayload = createEpidPayload(signatureComposite, sgType);

    // EPID devices may return a signature is a slightly different format than that
    // expected by the EPID verifier. Adjust the signature for these cases here, before
    // building the body for the verifier.

    final int sigWithHeaderNoCounts = 569;
    final int sigNoHeaderNoCounts = 565;
    final int sigWithHeaderWithCounts = 573;

    // Conversion cases:
    // 1) (siglength == SIG_WITH_HEADER_NO_COUNTS)
    //    remove first 4 bytes and add 8 zeroes on the end
    //    sver and blobid are prepended and sigRLVersion and n2 are not included
    // 2) (sigLength == SIG_NO_HEADER_NO_COUNTS)
    //    add 8 zeroes on the end (signature is missing sigRLVersion and n2 values)
    // 3) ((sigLength - SIG_WITH_HEADER_WITH_COUNTS) % 160 == 4)
    //    remove first 4 bytes (sver and blobid)

    byte[] adjSignature = signature;
    if (signature.length == sigWithHeaderNoCounts) {
      adjSignature = new byte[signature.length + 4];
      System.arraycopy(signature, 4, adjSignature, 0, signature.length - 4);
    } else if (signature.length == sigNoHeaderNoCounts) {
      adjSignature = new byte[signature.length + 8];
      System.arraycopy(signature, 0, adjSignature, 0, signature.length);
    } else if (((signature.length - sigWithHeaderWithCounts) % 160) == 4) {
      adjSignature = new byte[signature.length - 4];
      System.arraycopy(signature, 4, adjSignature, 0, signature.length - 4);
    }

    // Create the JSON encoded message body to send to verifier
    String msg = "{"
            + "\"groupId\":\"" + Base64.getEncoder().encodeToString(groupId) + "\""
            + ",\"msg\":\"" + Base64.getEncoder().encodeToString(signedPayload) + "\""
            + ",\"epidSignature\":\"" + Base64.getEncoder().encodeToString(adjSignature) + "\""
            + "}";
    return msg;
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
      default:
        return null;
    }
    return baos.toByteArray();
  }

}
