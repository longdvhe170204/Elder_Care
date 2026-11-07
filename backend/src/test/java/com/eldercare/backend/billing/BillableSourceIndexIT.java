package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.billing.FakeCostSources;
import com.eldercare.backend.support.fake.billing.FakeM11Config;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T009/T010: sự kiện nguồn chỉ cập nhật chỉ mục kỹ thuật; khoản do Hành chính lập ở T011. */
@Import(FakeM11Config.class)
class BillableSourceIndexIT extends MySqlIntegrationTestBase {
    private static final Instant PERFORMED_AT = Instant.parse("2026-11-04T19:30:00Z");
    private static final Instant INDEXED_AT = Instant.parse("2026-11-06T02:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    FakeCostSources sources;

    @Autowired
    TestClockControl clock;

    @Autowired
    BusinessTime time;

    @BeforeEach
    void setClock() {
        clock.setTo(INDEXED_AT);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactions);
    }

    private CostSourceBilled source(String type, long resident, String sourceId, String unit, String eventKey) {
        return new CostSourceBilled(
                type,
                sourceId,
                unit,
                resident,
                Optional.of(TestUsers.nextId()),
                BigDecimal.ONE,
                LocalDate.of(2026, 11, 4),
                PERFORMED_AT,
                eventKey);
    }

    private void publish(CostSourceBilled event) {
        transaction().executeWithoutResult(status -> sources.billed(event));
    }

    private void publish(Object event) {
        transaction().executeWithoutResult(status -> publisher.publishEvent(event));
    }

    private List<Map<String, Object>> indexed(long resident) {
        return jdbc.queryForList(
                "SELECT * FROM cost_source_index WHERE resident_id=? ORDER BY source_type,source_id,billing_unit",
                resident);
    }

    private void awaitCount(long resident, int count) {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(indexed(resident)).hasSize(count));
    }

    private int receiptCount(long resident) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM reminder_marker WHERE kind='M11.SOURCE_PROCESSED'"
                        + " AND entity_type='M11.BILLING_SOURCE' AND entity_id=?",
                Integer.class,
                resident);
    }

    private void noBusinessRecords(long resident) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE resident_id=?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cost_statement WHERE resident_id=?", Integer.class, resident))
                .isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAMILY_MEAL", "OVERNIGHT_STAY", "ACTIVITY_ATTENDANCE"})
    void FR_005_genericSourcesPreserveTheirOwnDateQuantityAndUnitWithoutCreatingCosts(String type) {
        long resident = TestUsers.nextId();
        var event = source(type, resident, "source-" + TestUsers.nextId(), "UNIT", "billed:" + resident);
        Instant before = time.now().truncatedTo(ChronoUnit.MICROS);

        publish(event);

        awaitCount(resident, 1);
        assertThat(indexed(resident)).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("source_type", type);
            assertThat(row).containsEntry("source_id", event.sourceId());
            assertThat(row).containsEntry("billing_unit", event.unit());
            assertThat(row).containsEntry("service_id", event.serviceId().orElseThrow());
            assertThat(row.get("medication_id")).isNull();
            assertThat(row.get("incurred_on").toString()).isEqualTo("2026-11-04");
            assertThat(((LocalDateTime) row.get("occurred_at")).toInstant(ZoneOffset.UTC))
                    .isEqualTo(PERFORMED_AT);
            assertThat(((LocalDateTime) row.get("updated_at")).toInstant(ZoneOffset.UTC))
                    .isBetween(before, time.now());
            assertThat((BigDecimal) row.get("current_quantity")).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(((Number) row.get("version")).intValue()).isZero();
        });
        assertThat(receiptCount(resident)).isEqualTo(1);
        noBusinessRecords(resident);
    }

    @Test
    void NFR_04_repeatedAndConcurrentDeliveriesAreHandledOnce() throws Exception {
        long resident = TestUsers.nextId();
        var event = source("FAMILY_MEAL", resident, "meal-" + resident, "UNIT", "repeat:" + resident);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                publish(event);
                return null;
            });
            var second = executor.submit(() -> {
                start.await();
                publish(event);
                return null;
            });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        publish(event);

        awaitCount(resident, 1);
        assertThat(receiptCount(resident)).isEqualTo(1);
        assertThat(((Number) indexed(resident).getFirst().get("version")).intValue())
                .isZero();
        noBusinessRecords(resident);
    }
}
