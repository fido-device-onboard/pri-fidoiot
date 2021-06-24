package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.fidoalliance.fdo.protocol.DiffieHellman.KeyExchange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DiffieHellmanTest {

  @ParameterizedTest
  @ValueSource(strings = {DiffieHellman.DH14_ALG_NAME, DiffieHellman.DH15_ALG_NAME})
  void test(String name) {
    DiffieHellman.KeyExchange a = DiffieHellman.buildKeyExchange(name);
    DiffieHellman.KeyExchange b = DiffieHellman.buildKeyExchange(name);

    BigInteger shA = a.computeSharedSecret(b.getMessage());
    BigInteger shB = b.computeSharedSecret(a.getMessage());

    assertEquals(shA, shB);

    DiffieHellman.KeyExchange c = new KeyExchange(b.getState());
    BigInteger shC = c.computeSharedSecret(a.getMessage());
    assertEquals(shB, shC);
  }
}