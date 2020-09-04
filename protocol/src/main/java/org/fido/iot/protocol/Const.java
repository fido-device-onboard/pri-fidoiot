// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Constants defined from the IoT Spec.
 */
public final class Const {

  //protocol message types

  //DI Protocol (non normative)
  public static final int DI_APP_START = 10;
  public static final int DI_SET_CREDENTIALS = 11;
  public static final int DI_SET_HMAC = 12;
  public static final int DI_DONE = 13;

  //TO0 Protocol
  public static final int TO0_HELLO = 20;
  public static final int TO0_HELLO_ACK = 21;
  public static final int TO0_OWNER_SIGN = 22;
  public static final int TO0_ACCEPT_OWNER = 23;

  //TO1 Protocol
  public static final int TO1_HELLO_RV = 30;
  public static final int TO1_HELLO_RV_ACK = 31;
  public static final int TO1_PROVE_TO_RV = 32;
  public static final int TO1_RV_REDIRECT = 33;

  //TO2 Protocol
  public static final int TO2_HELLO_DEVICE = 60;
  public static final int TO2_PROVE_OVHDR = 61;
  public static final int TO2_GET_OVNEXT_ENTRY = 62;
  public static final int TO2_OVNEXT_ENTRY = 63;
  public static final int TO2_PROVE_DEVICE = 64;
  public static final int TO2_SETUP_DEVICE = 65;
  public static final int TO2_AUTH_DONE = 66;
  public static final int TO2_AUTH_DONE2 = 67;
  public static final int TO2_DEVICE_SERVICE_INFO = 68;
  public static final int TO2_OWNER_SERVICE_INFO = 69;
  public static final int TO2_DONE = 70;
  public static final int TO2_DONE2 = 71;

  //Error message ID
  public static final int ERROR = 255;

  //protocol versions
  public static final int PROTOCOL_VERSION_100 = 100;

  //ErrorMessage
  public static final int EM_ERROR_CODE = 0;
  public static final int EM_PREV_MSG_ID = 1;
  public static final int EM_ERROR_STR = 2;
  public static final int EM_ERROR_TS = 3;
  public static final int EM_ERROR_UUID = 4;

  //protocol error codes uint16
  public static final int INVALID_JWT_TOKEN = 1;
  public static final int INVALID_OWNERSHIP_VOUCHER = 2;
  public static final int INVALID_OWNER_SIGN_BODY = 3;
  public static final int INVALID_IP_ADDRESS = 4;
  public static final int INVALID_GUID = 5;
  public static final int RESOURCE_NOT_FOUND = 6;
  public static final int MESSAGE_BODY_ERROR = 100;
  public static final int INVALID_MESSAGE_ERROR = 101;
  public static final int INTERNAL_SERVER_ERROR = 500;

  public static final String ERROR_CAUSE = " caused by ";
  public static final int ERROR_MAX_LENGTH = 512;

  //SigInfo
  public static final int SG_TYPE = 0;
  public static final int SG_INFO = 1;

  //device service info
  public static final int DVI_ISMORE = 0;
  public static final int DVI_VALUE = 1;

  //owner service info
  public static final int OVI_ISMORE = 0;
  public static final int OVI_ISDONE = 1;
  public static final int OVI_VALUE = 2;

  //ServiceInfo
  public static final int SVI_NAME = 0;
  public static final int SVI_VALUE = 1;

  //EAT Claims
  public static final long EAT_SDO_IOT = -19260421; // need IANA number
  public static final long EAT_MAROE_PREFIX = -19260422;// need IANA number
  public static final long EAT_NONCE = 9; //from spec
  public static final long EAT_UEID = 10; // from spec
  public static final int EAT_RAND = 1;
  public static final long CUPH_NONCE = -17760701;// need IANA number
  public static final long EUPH_NONCE = CUPH_NONCE;
  public static final long CUPH_PUBKEY = -17760702;// need IANA number

  public static final long COSEAES128CBC = -17760703; // need IANA number
  public static final long COSEAES128CTR = -17760704; // need IANA number
  public static final long COSEAES256CBC = -17760705; // need IANA number
  public static final long COSEAES256CTR = -17760706; // need IANA number

  public static final int SDO_CLAIM_KEXB = 3;

  public static final int ETM_AES128_GCM = 1;
  public static final int ETM_AES128_CCM = 10;

  public static final int ETM_AES128_CTR = 30; //not defined in spec
  public static final int ETM_AES256_CTR = 35; //not defined in spec
  public static final int ETM_AES128_CBC = 20; //not defined in spec
  public static final int ETM_AES256_CBC = 25; //not define in spec

  public static final long ETM_AES_IV = 5;
  public static final long ETM_AES_PLAIN_TYPE = 1;
  public static final long ETM_MAC_TYPE = 1;

  //HASH type indexes
  public static final int HASH_TYPE = 0;
  public static final int HASH = 1;

  //hashtype
  public static final int HASH_NA = 0; // not available (reuse protocol only)
  public static final int SHA_256 = 8; // Not defined in COSE
  public static final int SHA_384 = 14; // Not defined in COSE
  public static final int SHA_512 = 10; // only in reuse protocol

  //Java Algorithm names for HASH
  public static final String SHA_256_ALG_NAME = "SHA-256";
  public static final String SHA_384_ALG_NAME = "SHA-384";
  public static final String SHA_512_ALG_NAME = "SHA-512";

  //HMAC hashtype
  public static final int HMAC_SHA_256 = 5; // from COSE
  public static final int HMAC_SHA_384 = 6; // from COSE
  public static final int HMAC_SHA_256_KX = 108; // key exchange
  public static final int HMAC_SHA_512_KX = 110; // key exchange
  public static final int HMAC_SHA_384_KX = 114; // key exchange

  //Java Algorithm names for HMAC
  public static final String HMAC_256_ALG_NAME = "HmacSHA256";
  public static final String HMAC_384_ALG_NAME = "HmacSHA384";
  public static final String HMAC_512_ALG_NAME = "HmacSHA512";

  // PublicKey Indexes
  public static final int PK_TYPE = 0; //Public Key type
  public static final int PK_ENC = 1; // Public key encoding
  public static final int PK_BODY = 2; //Public key body

  //Public Key Types
  public static final int PK_RSA2048RESTR = 1; // Restricted RSA key/exponent
  public static final int PK_RSA = 4; // RSA key (unrestricted)
  public static final int PK_SECP256R1 = 13; // ECDSA secp256r1 = NIST-P-256 = prime256v1
  public static final int PK_SECP384R1 = 14; // ECDSA secp384r1 = NIST-P-384
  public static final int PK_EPIDv10 = 90; // Intel EPID, version 1.0
  public static final int PK_EPIDv11 = 91; // Intel EPID, version 1.1
  public static final int PK_EPIDv20 = 92; // Intel EPID, version 2.0

  //Java Algorithm names for Signatures
  public static final String RSA_256_ALG_NAME = "SHA256withRSA";
  public static final String RSA_384_ALG_NAME = "SHA384withRSA";
  public static final String ECDSA_256_ALG_NAME = "SHA256withECDSA";
  public static final String ECDSA_384_ALG_NAME = "SHA384withECDSA";

  // Public Key encodings
  public static final int PK_ENC_CRYPTO = 0; // crypto with its own encoding (e.g., EPID)
  public static final int PK_ENC_X509 = 1; // X509 DER encoding, applies to RSA and ECDSA
  public static final int PK_ENC_COSEEC = 2; // COSE EC2 encoding, applies to ECDSA
  public static final int PK_ENC_COSEKEY = 3;

  //Key factory algorithms
  public static final String EC_ALG_NAME = "EC";
  public static final String RSA_ALG_NAME = "RSA";
  public static final String X509_ALG_NAME = "X.509";
  public static final String VALIDATOR_ALG_NAME = "PKIX";

  //EC curve names
  public static final String SECP256R1_CURVE_NAME = "secp256r1";
  public static final String SECP384R1_CURVE_NAME = "secp384r1";

  //bit lengths of keys
  public static final int BIT_LEN_256 = 256;
  public static final int BIT_LEN_384 = 384;

  //OwnershipVoucher
  public static final int OV_HEADER = 0;
  public static final int OV_HMAC = 1;
  public static final int OV_DEV_CERT_CHAIN = 2;
  public static final int OV_ENTRIES = 3;

  //OVHeader
  public static final int OVH_VERSION = 0; // protocol version
  public static final int OVH_GUID = 1; // guid
  public static final int OVH_RENDEZVOUS_INFO = 2; // rendezvous instructions
  public static final int OVH_DEVICE_INFO = 3; // DeviceInfo
  public static final int OVH_PUB_KEY = 4; // mfg public key
  public static final int OVH_CERT_CHAIN_HASH = 5; // Hash of device certificate chain

  //RVVariable
  public static final int RV_DEV_ONLY = 0;
  public static final int RV_OWNER_ONLY = 1;
  public static final int RV_IP_ADDRESS = 2;
  public static final int RV_DEV_PORT = 3;
  public static final int RV_OWNER_PORT = 4;
  public static final int RV_DNS = 5;
  public static final int RV_SV_CERT_HASH = 6;
  public static final int RV_CLT_CERT_HASH = 7;
  public static final int RV_USER_INPUT = 8;
  public static final int RV_WIFI_SSID = 9;
  public static final int RV_WIFI_PW = 10;
  public static final int RV_MEDIUM = 11;
  public static final int RV_PROTOCOL = 12;
  public static final int RV_DELAY_SEC = 13;

  //RVProtocolValue
  public static final int RV_PROT_REST = 0;
  public static final int RV_PROT_HTTP = 1;
  public static final int RV_PROT_HTTPS = 2;
  public static final int RV_PROT_TCP = 3;
  public static final int RV_PROT_TLS = 4;
  public static final int RV_PROT_COAP_TCP = 5;
  public static final int RV_PROT_COAP_UDP = 6;

  //RVMediumValue
  public static final int RV_MED_ETH0 = 0;
  public static final int RV_MED_ETH1 = 1;
  public static final int RV_MED_ETH2 = 2;
  public static final int RV_MED_ETH3 = 3;
  public static final int RV_MED_ETH4 = 4;
  public static final int RV_MED_ETH5 = 5;
  public static final int RV_MED_ETH6 = 6;
  public static final int RV_MED_ETH7 = 7;
  public static final int RV_MED_ETH8 = 8;
  public static final int RV_MED_ETH9 = 9;
  public static final int RV_MED_WIFI0 = 10;
  public static final int RV_MED_WIFI1 = 11;
  public static final int RV_MED_WIFI2 = 12;
  public static final int RV_MED_WIFI3 = 13;
  public static final int RV_MED_WIFI4 = 14;
  public static final int RV_MED_WIFI5 = 15;
  public static final int RV_MED_WIFI6 = 16;
  public static final int RV_MED_WIFI7 = 17;
  public static final int RV_MED_WIFI8 = 18;
  public static final int RV_MED_WIFI9 = 19;
  public static final int RV_MED_ETH_ALL = 20;
  public static final int RV_MED_WIFI_ALL = 21;

  public static final int OVE_HASH_PREV_ENTRY = 0;
  public static final int OVE_HASH_HDR_INFO = 1;
  public static final int OVE_PUB_KEY = 2;

  //COSE Signature indexes
  public static final int COSE_SIGN1_PROTECTED = 0;
  public static final int COSE_SIGN1_UNPROTECTED = 1;
  public static final int COSE_SIGN1_PAYLOAD = 2;
  public static final int COSE_SIGN1_SIGNATURE = 3;

  //COSE Signature Types
  public static final int COSE_ES256 = -7;
  public static final int COSE_ES384 = -35;
  public static final int COSE_ES512 = -36; //not supported in spec
  public static final int COSE_RS256 = -257;
  public static final int COSE_RS384 = -258;
  //todo:EPID types still need defined

  //Common COSE Headers Parameters
  public static final long COSE_ALG = 1;

  //COSE empty_or_serialized_map
  public static final byte[] EMPTY_BYTE = new byte[0];

  //Stream Message (SM) indexes
  public static final int SM_LENGTH = 0;
  public static final int SM_MSG_ID = 1;
  public static final int SM_PROTOCOL_VERSION = 2;
  public static final int SM_PROTOCOL_INFO = 3;
  public static final int SM_BODY = 4;

  //Composite indexes
  public static final int FIRST_KEY = 0;
  public static final int SECOND_KEY = 1;
  public static final int THIRD_KEY = 2;
  public static final int FOURTH_KEY = 3;
  public static final int FIFTH_KEY = 4;
  public static final int SIXTH_KEY = 5;
  public static final int SEVENTH_KEY = 6;

  public static final int NO_KEYS = -1;

  //TO0 Demonstrate keys
  public static final int TO0_TO0D = 0;
  public static final int TO0_TO1D = 1;

  public static final int TO0D_VOUCHER = 0;
  public static final int TO0D_WAIT_SECONDS = 1;
  public static final int TO0D_NONCE3 = 2;

  //TO1D indexes
  public static final int TO1D_RV = 0;
  public static final int TO1D_TO0D_HASH = 1;

  //TransportProtocol (for ownerSign)
  public static final int BLOB_PROT_TCP = 1; //bare TCP stream
  public static final int BLOB_PROT_TLS = 2; //bare TLS stream
  public static final int BLOB_PROT_HTTP = 3;
  public static final int BLOB_PROT_COAP = 4; //Constrained Application Protocol
  public static final int BLOB_PROT_HTTPS = 5;
  public static final int BLOB_PROT_COAPS = 6; //Constrained Application Protocol (secure)

  public static final int BLOB_IP_ADDRESS = 0;
  public static final int BLOB_DNS = 1;
  public static final int BLOB_PORT = 2;
  public static final int BLOB_PROTOCOL = 3;

  //message data size sizes
  public static final int MAX_UINT16 = 65535;
  public static final long MAX_UINT32 = 4294967295L;
  public static final int MAX_UINT8 = 255;
  public static final int NONCE16_SIZE = 16;
  public static final int GUID_SIZE = 16;
  public static final int ARRAY5_TOKEN = -123; //0x85
  public static final int ARRAY1_TOKEN = -127; //0x81
  public static final int MIN_READ_SIZE = 4;
  public static final int MAX_READ_SIZE = 64 * 1024;
  public static final int DEFAULT = 0; //default length
  public static final Composite EMPTY_MESSAGE = Composite.fromObject(Collections.EMPTY_LIST);

  //SERVLET key
  public static final String DISPATCHER_ATTRIBUTE = "ProtocolDispatcher";

  //HTTP constants
  public static final int HTTP_INTERNAL_SERVER_ERROR = 500;
  public static final int HTTP_UNSUPPORTED_MEDIA_TYPE = 415;
  public static final int HTTP_LENGTH_REQUIRED = 411;
  public static final int HTTP_OK = 200;
  public static final String HTTP_BEARER = "Bearer";
  public static final String HTTP_AUTHORIZATION = "Authorization";
  public static final String HTTP_APPLICATION_CBOR = "application/cbor";
  public static final String HTTP_MESSAGE_TYPE = "Message-Type";

  //Protocol Info KEYS
  public static final String PI_TOKEN = "token";

  public static final byte[] X509_EC384_HEADER = new byte[]{
      48, 118, 48, 16, 6, 7, 42, -122, 72, -50, 61,
      2, 1, 6, 5, 43, -127, 4, 0, 34, 3, 98, 0, 4};

  public static final byte[] X509_EC256_HEADER = new byte[]{
      48, 89, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1,
      6, 8, 42, -122, 72, -50, 61, 3, 1, 7, 3, 66, 0, 4};

  //Key agreement algorithms
  public static final String ECDH_ALG_NAME = "ECDH";
  public static final String ECDH384_ALG_NAME = "ECDH384";
  public static final int ECDH_256_RANDOM_SIZE = 128 / 8;
  public static final int ECDH_384_RANDOM_SIZE = 384 / 8;

  public static final String AES128_CTR_HMAC256_ALG_NAME = "AES128/CTR/HMAC-SHA256";
  public static final String AES128_CBC_HMAC256_ALG_NAME = "AES128/CBC/HMAC-SHA256";
  public static final String AES256_CTR_HMAC384_ALG_NAME = "AES256/CTR/HMAC-SHA384";
  public static final String AES256_CBC_HMAC384_ALG_NAME = "AES256/CBC/HMAC-SHA384";

  public static final int IV_SIZE = 16;
  public static final int IV_SEED_SIZE = 12;
  public static final String AES = "AES";

  // stored values
  public static final String KEY_EXCHANGE_A = "A";
  public static final String KEY_EXCHANGE_B = "B";

  public static final byte[] KDF_STRING = "MarshalPointKDF".getBytes(StandardCharsets.US_ASCII);
  public static final byte[] PROV_CIPHER = "AutomaticProvisioning-cipher"
      .getBytes(StandardCharsets.US_ASCII);
  public static final byte[] PROV_HMAC = "AutomaticProvisioning-hmac"
      .getBytes(StandardCharsets.US_ASCII);

  public static final byte[] HMAC_ZERO = new byte[]{0};

  public static final char[] HEX_CHARS = new char[]{
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  //Device Credentials values
  public static final int DC_ACTIVE = 0;
  public static final int DC_PROTVER = 1;
  public static final int DC_HMAC_SECRET = 2;
  public static final int DC_DEVICE_INFO = 3;
  public static final int DC_GUID = 4;
  public static final int DC_RENDEZVOUS_INFO = 5;
  public static final int DC_PUBLIC_KEY_HASH = 6;

}
