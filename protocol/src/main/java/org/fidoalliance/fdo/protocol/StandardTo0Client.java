package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;
import org.fidoalliance.fdo.protocol.message.To0Hello;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;

public class StandardTo0Client extends HttpClient {

  private To0d to0d;
  private To2AddressEntries addressEntries;

  public To0d getTo0d() {
    return to0d;
  }

  public To2AddressEntries getAddressEntries() {
    return addressEntries;
  }

  public void setTo0d(To0d to0d) {
    this.to0d = to0d;
  }

  public void setAddressEntries(To2AddressEntries addressEntries) {
    this.addressEntries = addressEntries;
  }

  @Override
  public void initializeSession() {}

  @Override
  protected void generateHello() throws IOException {

    byte[] headerTag = getTo0d().getVoucher().getHeader();
    OwnershipVoucherHeader header =
      Mapper.INSTANCE.readValue(headerTag,OwnershipVoucherHeader.class);
    setInstructions(HttpUtils.getInstructions(header.getRendezvousInfo(), false));

    setRequest(new DispatchMessage());
    getRequest().setMsgType(MsgType.TO0_HELLO);
    getRequest().setMessage(Mapper.INSTANCE.writeValue(new To0Hello()));
    SimpleStorage storage = new SimpleStorage();
    storage.put(To0d.class, getTo0d());
    storage.put(To2AddressEntries.class,getAddressEntries());
    getRequest().setExtra(storage);

  }
}
