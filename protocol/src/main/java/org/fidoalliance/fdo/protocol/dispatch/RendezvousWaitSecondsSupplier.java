package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableSupplier;

public interface RendezvousWaitSecondsSupplier extends FailableSupplier<Long, IOException> {

}
