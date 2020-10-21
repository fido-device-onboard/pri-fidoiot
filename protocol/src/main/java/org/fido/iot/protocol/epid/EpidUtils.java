package org.fido.iot.protocol.epid;

import java.security.NoSuchAlgorithmException;
import org.fido.iot.protocol.Const;

public class EpidUtils {

  /**
   * Returns EPID group id length.
   *
   * @param sgType signature type
   * @return group id length
   */
  public int getEpidGroupIdLength(int sgType) {
    if (sgType == Const.SG_EPIDv10) {
      return Const.GID_LEN_EPIDv10;
    }
    if (sgType == Const.SG_EPIDv11) {
      return Const.GID_LEN_EPIDv11;
    }
    if (sgType == Const.SG_EPIDv20) {
      return Const.GID_LEN_EPIDv20;
    }
    throw new RuntimeException(new NoSuchAlgorithmException());
  }
}
