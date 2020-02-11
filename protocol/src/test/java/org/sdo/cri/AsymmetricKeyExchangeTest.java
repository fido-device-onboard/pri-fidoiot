// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AsymmetricKeyExchangeTest {

  private static final KeySpec keySpec2048 = new PKCS8EncodedKeySpec(
      Base64.getDecoder().decode(
          "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCmCLlk7AAkehGH"
              + "l5BIaXQ0y9I1QiRdZD6fUrXcS2c8xQvI/hYwgSk5w1Ai9/VP5L1WR3FvKO/DspUk"
              + "Oa3tg4r2gl1gTreRG3S4ENAinj9e/zqYpwxMD2P9aIMCXMvcC/e9324WZr04bCm4"
              + "ZcK+ED6kfM+hGm+ofnmEB8o34d1DAy3LYLik/n+auM/ZOcLMUH5K/3n3+QLaPkua"
              + "hTqi546Q0BSHjpStvr34OOUH1pP9/R5hxwCoD1Suh9iZ+qV7ogeUGDWEXSKKCw/s"
              + "LMMiRiR+l9yCmnqvn7dsNZvx+9SEIgSEk8sfucfQcWuRuOFdcKZGVwW52l2gOjgb"
              + "RrbcHPwbAgMBAAECggEAbdelt1O+gGNVh4vIw28olukk52Ebp8FzAHp6oAQuh3Cg"
              + "7FbUnUBF4tYJBSlf3HwqmpLnQ7AkdyaFLpMOMuzsmUsDtXw+SzyLyl4tJEnnCNT3"
              + "khgptYQVFYf37C8yGyC7xJy1OxK7KkG8lCaWYKFkn/OGdUGFD0ak46k3qiK9+99k"
              + "jGrY6BeOHKr4wd4LwSQ14NNhKckes4c3sdebD+DwAj0c1mISm0ZMuaw/kJBwfTLw"
              + "ywceyRTQhL+4ljWlcmCKi/t789g+zBc5JiJNJTkhbYCvJJadhuE8EKVTz72ZMkKS"
              + "ygo2qb4AYNZzWPVP3lJgsVt7aqwnZcVSwSZIFO1P4QKBgQDVslaXlt0oUxIpcBo0"
              + "PQBYrFqLkQ/g8qh7XKeWoG5aY22okk11h2N4I24lqtuc8EMxd0qa5RjlW1rzm7Kv"
              + "hKVGy4y93AD9XGxApH6rZxTGvY3/Ei+NRlSVtITBHCvJcK1mkwOXHt1mHrtOMJeB"
              + "s8+XHXLv+PnE1m52Oz+o5Ip/ywKBgQDG5vSeAwVt54yjawOMXj9FmRw5pcYcM/dD"
              + "7vZtnWaZeSlj16TdrFLUUurfUDoGQn90HFmD1/kcqLZED8BEL+IBmv3cZ1U3Ksk8"
              + "3ucRtKAcy5bTDrZEiOJAQBBi68Tay8bTF414m43SbFjCFQYXFMarq8e4o5mAL06j"
              + "eF/ZuR5K8QKBgQDPact2Uu3Rh9+m7LHiZhsB9jfkIyZgVeaF5gabwpeZj8wNxAL+"
              + "wiwfnXP5h36lMuZlCr4U3axuYV9D96lVhs/MiSDP3svvHlfL3l08DE80HyPFoWca"
              + "HltW+uGndapiWMVVO/hOI50M02NFnxBOybIBoInAtc3n6aFJjHRbf4eWRQKBgCS1"
              + "+/AuHpJqakq/NahyvLrbx5ncPvR1ou2m7VTN+GHbOwhyhLtlUuRdaXxOEmeVXnDP"
              + "yrXK5u+jNADb52CudchWEwTbDZ9hBkN14LAj5mlqKixKStnbaebx7BRV779yXQAZ"
              + "GMNp8oMj/D3WI4mfDAwjaHlruKkwEhOZ5BcLU8wBAoGBAKf4F++6PJQem0mWDZRB"
              + "GYZtfP/zEE9gW62u5ogeQwsF1CMpisSebzNEQG8naaGXBxmksPBE6JOIL5s2l2gA"
              + "tYZq3cxbFbon5Ek6Y2BkuTEE+x0vLKZVjtr6qBMFqDZfcFc98hIewUN7A60idjI7"
              + "f4MaZ1f3u0I158zX5F/2CEt5"));

  private static final KeySpec keySpec3072 = new PKCS8EncodedKeySpec(
      Base64.getDecoder().decode(
          "MIIG/QIBADANBgkqhkiG9w0BAQEFAASCBucwggbjAgEAAoIBgQDEeP/lO+vJPnN0"
              + "2StDIYSfrNmxyIMWftqNNjGZlKlNYeuV0QwmPT+1VN9r3O39tN0hTwCGXG9CfLVy"
              + "TOVoX/wljco5aEG6JOa7OdAv9n1T4BL2ZksNP4ku8PtErZJ0bThWqTtwRXbjKYCA"
              + "tgryUdJeuHIosSYVQc47kVvKFBFajcOOMQ4uBpKpaxpN3PrnLz+H6gOLbl4ov3OC"
              + "btPu6yAbKnpTBvIdEFvQfH51U6w9Mef7Kx8OU1Vxi6dlShOSWFPr7UkYa3pluAJr"
              + "n53S03cPGJTBwncntRPb0EpD6UgaW0uVGppCa+oFCDYDXmbaC/g2jKH5zjCd/hKn"
              + "N9D2sP+CM5oeUSvE+oXT3RXVGxROXtMHZ3xneU2yECQrPdmrQnoO+oRL5hqLjL54"
              + "VHafNXj9DP7F38zHgVdxLR0BwJyVmVXfe5m7E//+Bq+rjiDTCVm4GQnpHuPqf8Vd"
              + "3iyu3oilbaWBKchxaLHWzk5HQU9OKyaMa7F1I2NJozFx8HfuvD8CAwEAAQKCAYAp"
              + "CoWZ4wx7+1mu3PaWjVfg6wCGjLYlatZIjTuLM2yyR3Qo1YROMJkwTcxmAb6NMszG"
              + "enRHnON45ggZGksJpJNNXPMTTt37/l1cWtVMT5cagEI1ymR/HfiQH7K9Q+zF+VK9"
              + "d5h8+WW+ShsXx+89cy4YrzrZmTe1VQu5kcG00sBVRCvHdiPaJXFZho/2F+Upicsh"
              + "nxaid6mfpW6BnpkS3XbVdegbk+XbgGFNidP4ukPOfLBS3U0ddvbwQxXyqKix91Ui"
              + "qcA3nuYROTUPdwbRvL2cFJLCljVVU5S2Hjl8Ux53tZPhn2Wj7fbc5edHnK+LxWG+"
              + "g6jw7ZY2tf8n91ujgxYzrL6GWK1RGDWQIeW/DkLIoCWBctQ/GulPnSNHm/vRaGvg"
              + "aNGP6VqDj/anVZwwUbGLJtwnAJI5/HvIqgyN/8M+6xia6alajrDVm/bGPMfruwuO"
              + "yUQ5nhDazRW+7BwQPwV+kjrIB4Q5tAM4g8qeMY+5eXicjbr670UxI6XipSJ/DaEC"
              + "gcEA7SGgVppxeIrrzOPnr/ix7rcD2gahuHk6qPl+H3qbOvecdYbyWvatgnOVU4A0"
              + "bpBvnsGDCJ1bWbk4HlT7mrpmsCjxPafCahrgenoAleH0Jdfv3qyXDzuKmAh3UBZ1"
              + "8IIH+aUg0CnqmCoC+1yscJAhFG3R8r/xmnNpqNnNj2zbj2j0SS0LcQvgRhRbNVzy"
              + "ezsBFd2rXa3KTpWZtw4Skr7iyiylxkE2AsM9Ac4MxsnR6hVBJGvk0Cy3InDbiV8L"
              + "KmGZAoHBANQbJ3qDGZq5QbEYRSNGMZt1q9k98sOzycIZadiTIAKoBU6QanggeWFk"
              + "kOBDfZ8wVQVZD/ipKdviAlQbDWen4FBvOS6eDGJJn7ubvt+hNiuJ859a7y6YCXDA"
              + "TNZhaDEBMpnKMB6+H8bonmeIdc9kMigeYk8EAgYSHzQKDyflZup4MoA8cw7m9jyg"
              + "FMQ3RjHAq9nUY7B2QJm/Jpo/1VzhHuOZYt8bDoBLYYYkz7uOAYEV0diHMcw6ebsY"
              + "n3fKEFljlwKBwGpmMilptwxdWRxgDm846a2w1LuZSisZyiZTfDHns4sr2zooyMnj"
              + "8UqdTh1fYydRC6RyM7dD099YakPB4fs1NCpK1KPTiDpPwPVzlJur/P6V+30wd79P"
              + "+gnpko+O5HB8B9QOMcD1bdvEzf23QXBoW09tT0zary+cJYar4749G+8nKqVC3uVk"
              + "bMdFDv/Mqsh8WypSgjfQaPwFyCkI2XCa0evhSKjCTdfDfrY2nCj+GYAs9UPAXbTC"
              + "u5Fqjjb/PGVR0QKBwG635fm7qYfsoG/zPVPWdlqyKAx0m96wYOcjIO9ejV0Vm/+Q"
              + "vyMJwJKoTj/iuxZ3wknSXdXtZ95Rsel5XZv4M82I4Y6GDezYhbsG57IQkxhTIIrU"
              + "ivoFfxgOKY3Wtu6ts143/rptgGTyVI2y29xwbwYmJicc7nmAyCz/+z1WMEzAhYwP"
              + "AfiLc4nd/jkWqVubOZmxloAsmjgnYd0u6Q9crCUekk/n7b/mmXfOi2laDUr2ut2O"
              + "ZN44wAZ8KfH71mDy7QKBwQDYIf1+OmgIl0LHPu8M9zNUyBZb/pkHsyIffAk5Osyp"
              + "s7Jof/Rp2nZ4N8MNjWyCyj4EhejUuhIhi0ouWlydQdLvsTj4rMasNCTmVXDXpZ9Q"
              + "JZrMkfNHGqgQ6xSi2oy/LCFfGZuTDxdjcI4DdpnH9sI0XLXt8R0fhWRtTFaF1vuj"
              + "kS1gc0nA3hhCV0aod5jOfZQtfxk3VMyZ1YAB34QR2m7R5hKTtaJlSHUBHIqa12BK"
              + "78rTGTddeFZeggOGUD5mt/8="));

  private static RSAPrivateCrtKey pri2048;
  private static RSAPrivateCrtKey pri3072;
  private static RSAPublicKey pub2048;
  private static RSAPublicKey pub3072;
  private static SecureRandom random;

  @BeforeAll
  static void beforeAll() throws Exception {
    BouncyCastleLoader.load();
    random = SecureRandom.getInstance("SHA1PRNG");

    KeyFactory kf = KeyFactory.getInstance("RSA");

    pri2048 = (RSAPrivateCrtKey) kf.generatePrivate(keySpec2048);
    pub2048 = (RSAPublicKey) kf.generatePublic(
        new RSAPublicKeySpec(pri2048.getModulus(), pri2048.getPublicExponent()));

    pri3072 = (RSAPrivateCrtKey) kf.generatePrivate(keySpec3072);
    pub3072 = (RSAPublicKey) kf.generatePublic(
        new RSAPublicKeySpec(pri3072.getModulus(), pri3072.getPublicExponent()));
  }

  @Test
  @DisplayName("ASYMKEX2048")
  void generateSharedSecret_2k() {

    final KeyPair keys = new KeyPair(pub2048, pri2048);

    AsymmetricKeyExchange.Owner kxa = new AsymmetricKeyExchange.Owner(keys, random);
    AsymmetricKeyExchange.Device kxb = new AsymmetricKeyExchange.Device(keys, random);

    ByteBuffer xa = kxa.getMessage();
    ByteBuffer xb = kxb.getMessage();

    ByteBuffer shSeA = kxa.generateSharedSecret(xb);
    ByteBuffer shSeB = kxb.generateSharedSecret(xa);

    assertEquals(256 * 2 / 8, shSeA.remaining());
    assertEquals(shSeA, shSeB);
  }

  @Test
  @DisplayName("ASYMKEX3072")
  void generateSharedSecret_3k() {

    final KeyPair keys = new KeyPair(pub3072, pri3072);

    AsymmetricKeyExchange.Owner kxa = new AsymmetricKeyExchange.Owner(keys, random);
    AsymmetricKeyExchange.Device kxb = new AsymmetricKeyExchange.Device(keys, random);

    ByteBuffer xa = kxa.getMessage();
    ByteBuffer xb = kxb.getMessage();

    ByteBuffer shSeA = kxa.generateSharedSecret(xb);
    ByteBuffer shSeB = kxb.generateSharedSecret(xa);

    assertEquals(768 * 2 / 8, shSeA.remaining());
    assertEquals(shSeA, shSeB);
  }

  @Test
  @DisplayName("old SHA1 (DAL) support")
  void generateSharedSecret_sha1() throws Exception {

    Base64.Decoder b64d = Base64.getDecoder();

    // The only example we have of this is a snapshot of a test run.
    // Recycle the test keys that were used in that run for this test only.
    //
    RSAPrivateKey ownerPriKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
        .generatePrivate(new PKCS8EncodedKeySpec(b64d.decode(
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDNV+8PEi27UTvi"
                + "gmPAsdiP4lxvkpj4OOaJZpnqQBoqv00UtQmzsEngtKOa5TNj2XhnZVHBYBEuH5Bd"
                + "FNaXxGVh4ruUOdWj8wJ9I3uNI+sPIoesRjEOMOV9eC1EwhOjguBOrdmLedBkhBZi"
                + "JeyaQvC6RbDGptPs8aq2c8t4sto3MvjW8P8Fhbruz3/vh4VilSZMCpEQaj6TYFmn"
                + "vbfYJllnkNaJsbSHhMg0nbyCcg9o4f2FTYmRaKLIm6yYwm1TWIazSpxTSBMYNYkp"
                + "I8TlhWcFUtW/NxM6m6mT9HLLIiWKIpERlxOZwejjtL2GqNXN8hWa/hAVx0RpasB0"
                + "ji8z5+MfAgMBAAECggEAR8hbm4shTYAiTRaDxJLnP7VD1wUKUIujm5iwaYErJJJn"
                + "Ybu/vn779H9xSe6hvmABhTOYFt98YgFKuB9WvQs83bEjHbfyBTQoRJKo2DgcLdhG"
                + "UWTgNTPGpcvjF0w5DUntG9QOXoGg5eJ+6KSVR3/rI1LFb0xWfVgl804bbQAo5G5e"
                + "3SazMYgG+UbLOKEmz3BbYztivCvPoEP17sk5hkT2GrGefySrgOBEbJ4QjqYjoOxh"
                + "5W+ZzcxFywG47oI4y++oFFpNnQF8/wQdZahFkPj+kKdkDA5pm4woOVPdvgve75sn"
                + "nbPOPohJRQJLFPg4y7POebxQZX2MmiDAcLzcNn+hwQKBgQDmmnDOTIJtxjiCt+Tt"
                + "IpNTi7dql3YdijjeiT96o2VcUWFx4vIK8imEpeM+jXvDXvGfV5iMnbG2f4zD6pWs"
                + "Fvdy2dT83PCu6PTYPTLBByBN4hMRm62mLBp9/8wEEHkUuMP1YWYtJ1TWOiZDp1zH"
                + "aDUgvHZoS3cJVEjP5H5aMy+b8QKBgQDj9VODHk3fQd5MC9nBsxaCMK762n95HBDF"
                + "1/Lqi5gQjk942IFe1VehAiE20G/p2czvx6XYLGOXNqBUgih4wixsuoZt4QrTc4Ov"
                + "zojnKNpumgIwT5SJcIEsO4u8jAYXPNd0R1Eazij1GncTjCtL1XNcjFinyt9qWlSH"
                + "qlntrrnADwKBgQDiC+4cK5+G39GxQXYkhcoJEWIgGIxt9Ho+michnl3Tmup8asx8"
                + "sljEcKBLRwFUyBLt8T3WSRZVIL7ppWBGKtUxPhqsLB+6NR0dgw/na4bdEYlDr2kP"
                + "BRiK/l4MdeFa0Ks2M92m0XE19hZgJwSpa5r4lgUzwxXSjU46i27YGGjI4QKBgAca"
                + "S8HYHeEI8l44OE66v8bKsYkLadFiOHuRoPYB53kMlhlT1aQYzaQsptBIalRE/wex"
                + "ls9HgsrhXtmDPLOWiT/SCAJAcs/n14CQ1N8u/K9ZiRvsPtqRcNTp/g6f5kivEp+C"
                + "4VoOdmQ8sN0hDvBL9E476T9BKPjtW5jmi+rF+O+tAoGABPnQrl4kKCEDPtNn3+Xb"
                + "d8zKETLZ511j0HuIvT2/rCZlAEKbqQVxjVDf78GLBGXL3rOEsiuaqnYJtzEtpPRH"
                + "AR8ogC2RL+g05fEcCQLmVJPjkzOqZyuarVVb2wWo+HZcJddjiAYSja7HjYHdLdYi"
                + "+SFXoTRaPUN7I2Hone1QO+w=")));
    Certificate ownerCert = CertificateFactory.getInstance("X.509")
        .generateCertificate(new ByteArrayInputStream(b64d.decode(
            "MIIDYDCCAkigAwIBAgIJALUU7oFnPfZLMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV"
                + "BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX"
                + "aWRnaXRzIFB0eSBMdGQwHhcNMTgxMTE1MTc1MDU3WhcNMjgxMTEyMTc1MDU3WjBF"
                + "MQswCQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50"
                + "ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB"
                + "CgKCAQEAzVfvDxItu1E74oJjwLHYj+Jcb5KY+DjmiWaZ6kAaKr9NFLUJs7BJ4LSj"
                + "muUzY9l4Z2VRwWARLh+QXRTWl8RlYeK7lDnVo/MCfSN7jSPrDyKHrEYxDjDlfXgt"
                + "RMITo4LgTq3Zi3nQZIQWYiXsmkLwukWwxqbT7PGqtnPLeLLaNzL41vD/BYW67s9/"
                + "74eFYpUmTAqREGo+k2BZp7232CZZZ5DWibG0h4TINJ28gnIPaOH9hU2JkWiiyJus"
                + "mMJtU1iGs0qcU0gTGDWJKSPE5YVnBVLVvzcTOpupk/RyyyIliiKREZcTmcHo47S9"
                + "hqjVzfIVmv4QFcdEaWrAdI4vM+fjHwIDAQABo1MwUTAdBgNVHQ4EFgQUnfe65kiL"
                + "OcKVvqW5Sf2fwGBzZ1EwHwYDVR0jBBgwFoAUnfe65kiLOcKVvqW5Sf2fwGBzZ1Ew"
                + "DwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAVE/ZfzbUiNSwSbPC"
                + "RE/Bzrjz8GLdOyrHfQVSJLIWgylLeAKaTedDz8w0lD55xNxszeeJ2KES7f7nE2rR"
                + "Swi9KsQxfxlKGhB4IwFh5LuALu3HmpDfFJVOWDgiqxRWU7lQgqjqTxhisudTu6e1"
                + "pgKP3fu/GaUA8FZUFAC0+3JobTqYyW/jdJI4iFIK2uMIY+4SdPxtmv6NNj34Tdmg"
                + "+8m90607DDBmGeigmusR8R3v4X6o++8Q2j+44nUxO7s8KXUzy69l7+HM+ljm4Bp2"
                + "5JgxzTvyIcMkqkhzRqnnm0831U5ie/p57rdR1H8QTkqvGULq2IrKot9rCRFa9nth"
                + "diVJcQ==")));
    RSAPublicKey ownerPubKey = (RSAPublicKey) ownerCert.getPublicKey();

    byte[] xa = b64d.decode("2a/HDlS2601Uc+1nO4+8we0/y/4Lp8npiZftZkMc1B4=");
    byte[] xb = b64d.decode(
        "KHfjAMNxQvKSwV+jG8bhpOr0iCQtZSA14kFo19Mn5Dg4UpeO31TXk7MxrfwIwjw9sd0iPuKcqm"
            + "KJV4OLUlpXCmHaRJQLz2rOjFOehF35Vpm5hvJmM3VdEET/ydwuE1wcwtshdml2jwxQfZ"
            + "+GCYtLdIPTVQK/EhlyiHjLYyV8iy1xjWGhtdT6gIwsBn+4qHHdrsQ32D4jRq6ZwuYsKR"
            + "lm0qvZKSxmIcg2aaGKcL9S+xTVtrsFgXQA5X7QdJtFb773BONsCrTFkYcTnNAHFXyj7Q"
            + "fMW0pWNBkhhhQROL3NyJyBlNwvOxPH5BmfTFIqjWENM5v1vPPmiMfuZ+/+KmAa4A==");

    final KeyPair keys = new KeyPair(ownerPubKey, ownerPriKey);

    AsymmetricKeyExchange.Owner kxa = new AsymmetricKeyExchange.Owner(keys, random);
    AsymmetricKeyExchange.Device kxb = new AsymmetricKeyExchange.Device(keys, random);

    kxa.setXa(ByteBuffer.wrap(xa));
    ByteBuffer shSeA = kxa.generateSharedSecret(ByteBuffer.wrap(xb));
    assertEquals(512 / 8, shSeA.remaining());

    kxb.setXb(ByteBuffer.wrap(xb));
    ByteBuffer shSeB = kxb.generateSharedSecret(ByteBuffer.wrap(xa));
    assertEquals(shSeA, shSeB);
  }
}
