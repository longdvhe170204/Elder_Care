package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.billing.internal.item.domain.CostRecalculation;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TableRows;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T005: dữ liệu tài chính M11 đi qua bảng/API chung thật, không nghiệm thu producer T020/T021. */
class CostSharedRowsIT extends MySqlIntegrationTestBase {
    private static final Instant AT = Instant.parse("2026-11-04T01:00:00Z");

    @Autowired
    StatusHistoryApi history;

    @Autowired
    ReminderMarkers markers;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    JdbcTemplate jdbc;

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactions);
    }

    private CostRecalculation change() {
        return new CostRecalculation(
                new CostRecalculation.Calculation(new BigDecimal("1.25"), null, new BigDecimal("100.00"), null),
                new CostRecalculation.Calculation(
                        new BigDecimal("1.25"),
                        new BigDecimal("1200"),
                        new BigDecimal("100.00"),
                        new BigDecimal("1500")),
                "UNIT_PRICE_AVAILABLE",
                true);
    }

    private long add(long item, long source) {
        return add(item, source, change());
    }

    private long add(long item, long source, CostRecalculation details) {
        return transaction()
                .execute(status -> history.add(
                        CostRecalculation.TRACK,
                        item,
                        new HistoryEntry<>(
                                HistoryKind.POINT,
                                "CHECKED",
                                "DRAFT",
                                null,
                                AT,
                                "RECALCULATE",
                                1L,
                                false,
                                new Reason("Đã có đơn giá nguồn"),
                                "M02.PRICE_VERSION",
                                source,
                                details,
                                null,
                                "M11.RECALCULATION:" + item + ":" + source)));
    }

    @Test
    void BR_M11_05_recalculationKeepsBeforeAfterCauseActorAndBasisWithoutLosingPrecision() {
        long item = TestUsers.nextId(), source = TestUsers.nextId();
        long id = add(item, source);
        assertThat(history.list(CostRecalculation.TRACK, item)).singleElement().satisfies(row -> {
            assertThat(row.id()).isEqualTo(id);
            assertThat(row.startsAt()).isEqualTo(AT);
            assertThat(row.actorId()).isEqualTo(1L);
            assertThat(row.basisType()).isEqualTo("M02.PRICE_VERSION");
            assertThat(row.basisId()).isEqualTo(source);
            assertThat(row.valueBefore()).isEqualTo("CHECKED");
            assertThat(row.value()).isEqualTo("DRAFT");
            assertThat(row.details())
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(change());
        });
    }

    @Test
    void FR_013_sharedHistoryRejectsMoneyOrQuantityOutsideStoragePrecision() {
        // số lượng, đơn giá, hệ số, số tiền
        TableRows.csv(
                r -> rejectedOutsidePrecision(r[0], r[1], r[2], r[3]),
                "1.2555,1200,100.00,1500",
                "1.25,1200.5,100.00,1500",
                "1.25,1200,100.001,1500",
                "1.25,1200,100.00,1500.5",
                "1.25,1200,100.00,1000000000000000",
                "1000000000,1200,100.00,1500",
                "1.25,1200,1000.00,1500");
    }

    private void rejectedOutsidePrecision(String quantity, String price, String factor, String amount) {
        long item = TestUsers.nextId();
        var invalid = new CostRecalculation(
                change().before(),
                new CostRecalculation.Calculation(
                        new BigDecimal(quantity),
                        new BigDecimal(price),
                        new BigDecimal(factor),
                        new BigDecimal(amount)),
                "SOURCE_CHANGED",
                true);
        assertThatThrownBy(() -> add(item, TestUsers.nextId(), invalid)).isInstanceOf(InvalidValueException.class);
        assertThat(history.list(CostRecalculation.TRACK, item)).isEmpty();
    }

    @Test
    void DBR_17_recalculationHistoryCannotBeEditedOrDeletedEvenByAdministrator() throws Exception {
        long id = add(TestUsers.nextId(), TestUsers.nextId());
        assertThatThrownBy(() -> jdbc.update("UPDATE status_history SET details=JSON_OBJECT() WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        try (var connection = adminConnection();
                var update = connection.prepareStatement("UPDATE status_history SET details=JSON_OBJECT() WHERE id=?");
                var delete = connection.prepareStatement("DELETE FROM status_history WHERE id=?")) {
            update.setLong(1, id);
            delete.setLong(1, id);
            assertThatThrownBy(update::executeUpdate).isInstanceOf(SQLException.class);
            assertThatThrownBy(delete::executeUpdate).isInstanceOf(SQLException.class);
        }
    }
}
