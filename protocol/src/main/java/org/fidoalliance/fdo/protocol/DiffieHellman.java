package org.fidoalliance.fdo.protocol;

import java.math.BigInteger;
import java.security.SecureRandom;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

public final class DiffieHellman {

  public static final String DH14_ALG_NAME = "DHKEXid14";
  public static final int DH14_RANDOM_SIZE_IN_BITS = 256;

  public static final String DH15_ALG_NAME = "DHKEXid15";
  public static final int DH15_RANDOM_SIZE_IN_BITS = 768;

  // DH P, G values come from RFC3526.
  static final BigInteger DH14_P = new BigInteger(
      "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
          + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
          + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
          + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
          + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
          + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
          + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
          + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
          + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
          + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
          + "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);
  static final BigInteger DH14_G = BigInteger.TWO;

  static final BigInteger DH15_P = new BigInteger(
      "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
          + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
          + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
          + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
          + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
          + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
          + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
          + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
          + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
          + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
          + "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64"
          + "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
          + "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B"
          + "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
          + "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31"
          + "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF", 16);
  static final BigInteger DH15_G = BigInteger.TWO;

  static final int RADIX_HEX = 16;

  /**
   * Build a new DH key exchange matching the supplied config name.
   *
   * @param name The name of the config to apply, which may be one of
   *             ("DHKEXid14","DHKEXid15")
   * @return The new key exchange object.
   */
  public static KeyExchange buildKeyExchange(String name) {
    switch (name) {
      case DH14_ALG_NAME:
        return new KeyExchange(DH14_P, DH14_G, DH14_RANDOM_SIZE_IN_BITS);
      case DH15_ALG_NAME:
        return new KeyExchange(DH15_P, DH15_G, DH15_RANDOM_SIZE_IN_BITS);
      default:
        throw new IllegalArgumentException();
    }
  }

  public static final class KeyExchange implements Destroyable {

    private final BigInteger myP;
    private final BigInteger myG;
    private BigInteger mySecret;

    private KeyExchange(BigInteger p, BigInteger g, int randomSizeInBits) {
      myP = p;
      myG = g;
      mySecret = new BigInteger(randomSizeInBits, new SecureRandom());
    }

    /**
     * Build a new object from a stored state.
     *
     * @param state the state.
     */
    public KeyExchange(Composite state) {
      myP = new BigInteger(state.getAsString(Const.FIRST_KEY), RADIX_HEX);
      myG = new BigInteger(state.getAsString(Const.SECOND_KEY), RADIX_HEX);
      mySecret = new BigInteger(state.getAsString(Const.THIRD_KEY), RADIX_HEX);
    }

    public BigInteger getMessage() {
      return myG.modPow(mySecret, myP);
    }

    public BigInteger computeSharedSecret(BigInteger otherMessage) {
      return otherMessage.modPow(mySecret, myP);
    }

    /**
     * Return the serialized state of this object so it can be restored later.
     */
    public Composite getState() {
      return Composite.newArray()
          .set(Const.FIRST_KEY, myP.toString(RADIX_HEX))
          .set(Const.SECOND_KEY, myG.toString(RADIX_HEX))
          .set(Const.THIRD_KEY, mySecret.toString(RADIX_HEX));
    }

    @Override
    public void destroy() throws DestroyFailedException {
      mySecret = null;
    }

    @Override
    public boolean isDestroyed() {
      return null == mySecret;
    }
  }
}
