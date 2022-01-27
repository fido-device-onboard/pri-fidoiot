package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.To0d;

public interface RvBlobStorageFunction extends
    FailableBiFunction<To0d, CoseSign1, Long, IOException> {

}
