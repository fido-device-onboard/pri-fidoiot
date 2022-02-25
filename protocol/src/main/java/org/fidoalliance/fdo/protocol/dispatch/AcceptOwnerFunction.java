package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.util.Date;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableSupplier;

public interface AcceptOwnerFunction extends FailableBiFunction<String, Long, Date, IOException> {

}
