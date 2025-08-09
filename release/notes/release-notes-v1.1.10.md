v1.1.10

This release includes critical security fixes, library upgrades, and enhancements for the Protocol Reference Implementation (PRI) component of FIDO Device Onboard.

## Components

### Protocol Reference Implementation (PRI)

[pri-fidoiot](https://github.com/fido-device-onboard/pri-fidoiot) is a Java-based implementation of the components defined in the [FIDO Device Onboard Specification](https://fidoalliance.org/specs/FDO/FIDO-Device-Onboard-PS-v1.1-20220419/).

**Supported Cryptographic Modes:**

- **Signing Keys:** ECDSA NIST P-256, ECDSA NIST P-384, RSA2048RESTR, Intel EPID 1.1
- **Key Exchanges:** ECDH256, ECDH384, ASYMKEX2048, ASYMKEX3072, DHKEXid14, DHKEXid15
- **Ciphers:** AES128/CTR/HMAC-SHA256, AES128/HMAC-SHA256, AES256/CTR/HMAC-SHA384, AES256/HMAC-SHA384, AES-CCM-64-128-128, AES-CCM-64-128-256, AES128GCM, AES256GCM, RSA/NONE/OAEPWithSHA256AndMGF1Padding
- **Public Key Encoding:** Crypto, X509, COSEKey, X5 Chain
- **COSE Signature Types:** ES256, ES384, RS2048

---

## New Features

- Skip TLS SAN checks for self-signed certificates.
- Improved Diffie-Hellman key exchange handling.

---

## Fixed Issues

### Security Fixes

- [CVE-2024-34750](https://nvd.nist.gov/vuln/detail/CVE-2024-34750)
- [CVE-2024-50379](https://nvd.nist.gov/vuln/detail/CVE-2024-50379)
- [CVE-2024-47554](https://nvd.nist.gov/vuln/detail/CVE-2024-47554)

### Dependency Updates

- Upgraded MySQL, Protobuf, and Commons-IO
- Updated GitHub Actions and builds workflows

### Other Fixes

- Reverted incorrect `service.yml` changes

---

## Known Issues

---

## SHA256 Checksum

*Following SHA256 checksum is calculated using sha256sum tool:*

```{text}
{{SHA_PRI}} - pri-fidoiot-v1.1.10.tar.gz
{{SHA_NOTICES}} - pri-fidoiot-NOTICES-v1.1.10.tar.gz
```

---

## Documentation

https://fido-device-onboard.github.io/docs-fidoiot/1.1.10
