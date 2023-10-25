// package org.fidoalliance.fdo.protocol;

// public class ManufacturingInfoTest {

// }

package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.UUID;
import org.apache.commons.codec.DecoderException;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.serialization.ManufacturingInfoDeserializer;
import org.junit.jupiter.api.Test;

public class ManufacturingInfoTest {
    @Test
    public void Test() throws DecoderException, IOException {

        ManufacturingInfoDeserializer obj = new ManufacturingInfoDeserializer();

        String[] invalidStrings = {
                "",
                "none",
                "NaN",
                "undefined",
                "undef",
                "null",
                "NULL",
                "(null)",
                "nil",
                "NIL",
                "true",
                "false",
                "True",
                "False",
                "TRUE",
                "FALSE",
                "None",
                "????",
                "???????????????????????????????????????????????????????????????????????????????",
                "??????????",
                "??? ??? ???",
                "'",
                "\"",
                "''",
                "\"\"",
                "'\"'",
                "\"''''\"'\"",
                "\"'\"'\"''''\"",
                "1;DROP TABLE users",
                "1'; DROP TABLE users-- 1",
                "'; EXEC sp_MSForEachTable 'DROP TABLE ?'; --",
                "' OR 1=1 -- 1",
                "' OR '1'='1",
                "1'1",
                "1 exec sp_ (or exec xp_)",
                "1 and 1=1",
                "1' and 1=(select count(*) from tablenames); --",
                "1 or 1=1",
                "1' or '1'='1",
                " ",
                "%",
                "_",
                "-",
                "--",
                "--version",
                "--help",
                "$USER",
                "/dev/null; touch /tmp/blns.fail ; echo",
                "`touch /tmp/blns.fail`",
                "$(touch /tmp/blns.fail)",
                "@{[system \"touch /tmp/blns.fail\"]}",
                "() { 0; }; touch /tmp/blns.shellshock1.fail;",
                "() { _; } >_[$($())] { touch /tmp/blns.shellshock2.fail; }",
                "<<< %s(un='%s') = %u",
                "+++ATH0",
                "%.1024d",
                "%.2048d",
                "%.4096d",
                "%.8200d",
        };

        for (String invalid : invalidStrings) {
            assertFalse(obj.isValidString(invalid), "Expected false for invalid string: " + invalid);
        }
    }
}
