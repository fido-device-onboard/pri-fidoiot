package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.junit.jupiter.api.Test;

public class VoucherTest {
  @Test
  public void Test() throws DecoderException, IOException {

    OwnershipVoucher voucher = new OwnershipVoucher();
    OwnershipVoucherHeader header = new OwnershipVoucherHeader();
    header.setDeviceInfo("sample");
    header.setVersion(ProtocolVersion.current());
    header.setGuid(Guid.fromRandomUuid());
    voucher.setHeader(Mapper.INSTANCE.writeValue(header));

    byte[] data = Mapper.INSTANCE.writeValue(voucher);
    String str = Hex.encodeHexString(data);
    System.out.println(str);

    //OwnershipVoucher voucher1 = Mapper.INSTANCE.readObject(data,OwnershipVoucher.class);


  }
}
