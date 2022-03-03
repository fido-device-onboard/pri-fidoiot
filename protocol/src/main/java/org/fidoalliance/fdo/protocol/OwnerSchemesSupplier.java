package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.function.FailableSupplier;

public interface OwnerSchemesSupplier extends FailableSupplier<List<String>, IOException> {

}
