package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.internal.pricing.service.CostPricingService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T006a: proxy transaction thật và nguồn đọc 004 thật; không tạo fake hoặc ghi bảng nguồn B. */
class CostPricingTransactionIT extends MySqlIntegrationTestBase {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 6);

    @Autowired
    CostPricingService pricing;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void BR_M11_02_priceSelectionRequiresTheCallersWriteTransaction() {
        assertThatThrownBy(() -> pricing.event(TestUsers.nextId(), TestUsers.nextId(), DATE, BigDecimal.ONE))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void Q_306_readingAMissingPriceInsideATransactionDoesNotCreateCostRecords() {
        long resident = TestUsers.nextId(), service = TestUsers.nextId();
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            int statements = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM cost_statement WHERE resident_id=?", Integer.class, resident);
            int items =
                    jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE resident_id=?", Integer.class, resident);
            var quote = pricing.event(resident, service, DATE, BigDecimal.ONE);
            assertThat(quote.missingUnitPrice()).isTrue();
            assertThat(quote.amount()).isNull();
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM cost_statement WHERE resident_id=?", Integer.class, resident))
                    .isEqualTo(statements);
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM cost_item WHERE resident_id=?", Integer.class, resident))
                    .isEqualTo(items);
            status.setRollbackOnly();
        });
    }
}
