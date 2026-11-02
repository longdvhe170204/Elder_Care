package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.care.api.VitalSignPort;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;

class BR_M04_10_VitalSignPortTransactionIT extends MySqlIntegrationTestBase {
    @Autowired
    VitalSignPort port;

    @Test
    void BR_M04_10_requiresTheCareTaskTransaction() {
        assertThatThrownBy(() -> port.record(1L, 1L, Instant.EPOCH, 1L, Map.of("VALUE", BigDecimal.ONE), null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
