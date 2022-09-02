// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.db.To0Scheduler;
import org.fidoalliance.fdo.protocol.dispatch.AcceptOwnerFunction;

import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;
import org.fidoalliance.fdo.protocol.message.To0AcceptOwner;
import org.fidoalliance.fdo.protocol.message.To0Hello;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;

public class StandardTo0Client extends HttpClient {

  private To0d to0d;
  private To2AddressEntries addressEntries;
  private static final LoggerService logger = new LoggerService(StandardTo0Client.class);

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
  protected void finishedOk() {
    To0AcceptOwner acceptOwner = getResponse().getExtra().get(To0AcceptOwner.class);
    if (acceptOwner != null) {
      try {
        OwnershipVoucherHeader header = Mapper.INSTANCE.readValue(to0d.getVoucher().getHeader(),
            OwnershipVoucherHeader.class);
        Config.getWorker(AcceptOwnerFunction.class).apply(header.getGuid().toString(),
            acceptOwner.getWaitSeconds());
      } catch (IOException e) {
        logger.error("Failed to update voucher wait seconds " + e.getMessage());
      }
    }

  }

  @Override
  public void run() {
    try {
      super.run();
    } catch (Exception e) {
      logger.error("To0 client error " + e.getMessage());
    }

  }


  @Override
  public void initializeSession() {
  }

  @Override
  protected void generateHello() throws IOException {

    byte[] headerTag = getTo0d().getVoucher().getHeader();
    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(headerTag, OwnershipVoucherHeader.class);
    setInstructions(HttpUtils.getInstructions(header.getRendezvousInfo(), false));

    //remove any instruction that are rvbypass
    getInstructions().removeIf(n -> n.isRendezvousBypass());
    setRequest(new DispatchMessage());
    getRequest().setMsgType(MsgType.TO0_HELLO);
    getRequest().setMessage(Mapper.INSTANCE.writeValue(new To0Hello()));
    SimpleStorage storage = new SimpleStorage();
    storage.put(To0d.class, getTo0d());
    storage.put(To2AddressEntries.class, getAddressEntries());
    getRequest().setExtra(storage);

  }
}
