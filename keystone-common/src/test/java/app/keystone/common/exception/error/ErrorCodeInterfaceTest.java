package app.keystone.common.exception.error;

import app.keystone.common.exception.error.ErrorCode.Client;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ErrorCodeInterfaceTest {

    @Test
    void testI18nKey() {
        String i18nKey = Client.COMMON_FORBIDDEN_TO_CALL.i18nKey();
        Assertions.assertEquals("Client.COMMON_FORBIDDEN_TO_CALL", i18nKey);
    }
}
