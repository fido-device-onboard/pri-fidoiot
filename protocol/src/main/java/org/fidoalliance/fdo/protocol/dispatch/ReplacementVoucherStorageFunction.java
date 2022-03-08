package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;

public interface ReplacementVoucherStorageFunction extends
    FailableBiFunction<OwnershipVoucher, OwnershipVoucher, String , IOException> {

}