package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.BillableCareResultRecorded;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationBilledEvent;
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
import org.junit.jupiter.params.provider.EnumSource;
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
    void FR_005_sourceWithoutAServiceIsStillIndexedForLaterPriceResolution() {
        long resident = TestUsers.nextId();
        var event = new CostSourceBilled(
                "FAMILY_MEAL",
                "meal-" + resident,
                "UNIT",
                resident,
                Optional.empty(),
                BigDecimal.ONE,
                LocalDate.of(2026, 11, 4),
                PERFORMED_AT,
                "meal:" + resident);

        publish(event);

        awaitCount(resident, 1);
        assertThat(indexed(resident).getFirst().get("service_id")).isNull();
        noBusinessRecords(resident);
    }

    @Test
    void FR_006_eachOvernightUnitKeepsItsOwnIdentityForTheSameStay() {
        long resident = TestUsers.nextId();
        String stay = "stay-" + TestUsers.nextId();
        var first = source("OVERNIGHT_STAY", resident, stay, "2026-11-04", "night:" + resident + ":1");
        var second = new CostSourceBilled(
                "OVERNIGHT_STAY",
                stay,
                "2026-11-05",
                resident,
                first.serviceId(),
                BigDecimal.ONE,
                LocalDate.of(2026, 11, 5),
                PERFORMED_AT.plusSeconds(86_400),
                "night:" + resident + ":2");

        publish(first);
        publish(second);

        awaitCount(resident, 2);
        assertThat(indexed(resident))
                .extracting(row -> row.get("billing_unit"))
                .containsExactly("2026-11-04", "2026-11-05");
        assertThat(receiptCount(resident)).isEqualTo(2);
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

    @Test
    void NFR_04_lateInitialDeliveryDoesNotRestoreAnAlreadyCancelledSource() {
        long resident = TestUsers.nextId();
        var event = source("FAMILY_MEAL", resident, "meal-" + resident, "UNIT", "initial:" + resident);
        publish(event);
        awaitCount(resident, 1);
        jdbc.update("UPDATE cost_source_index SET cancelled=TRUE,version=3 WHERE resident_id=?", resident);
        var late = new CostSourceBilled(
                event.sourceType(),
                event.sourceId(),
                event.unit(),
                resident,
                event.serviceId(),
                event.quantity(),
                event.incurredOn(),
                event.occurredAt(),
                "late-copy:" + resident);

        publish(late);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(receiptCount(resident)).isEqualTo(2));
        assertThat(jdbc.queryForObject(
                        "SELECT cancelled FROM cost_source_index WHERE resident_id=?", Boolean.class, resident))
                .isTrue();
        assertThat(((Number) indexed(resident).getFirst().get("version")).intValue())
                .isEqualTo(3);
        noBusinessRecords(resident);
    }

    @Test
    void NFR_04_longEventKeysAreNotTruncatedIntoTheSameReceipt() {
        long resident = TestUsers.nextId();
        String common = "delivery:" + "x".repeat(1000);
        var first = source("FAMILY_MEAL", resident, "meal-" + resident, "UNIT", common + "A");
        var second = new CostSourceBilled(
                first.sourceType(),
                first.sourceId(),
                first.unit(),
                resident,
                first.serviceId(),
                first.quantity(),
                first.incurredOn(),
                first.occurredAt(),
                common + "B");

        publish(first);
        publish(second);

        awaitCount(resident, 1);
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(receiptCount(resident)).isEqualTo(2));
    }

    @Test
    void FR_005_careResultUsesPerformedTimeAndKeepsTheActualQuantity() {
        long resident = TestUsers.nextId(), result = TestUsers.nextId(), service = TestUsers.nextId();
        var context =
                new CareEventContext("M04.CARE_RESULT", result, "RECORDED", INDEXED_AT, Optional.empty(), List.of());
        var event = new BillableCareResultRecorded(
                result, resident, TestUsers.nextId(), service, new BigDecimal("2.25"), PERFORMED_AT, context);

        publish(event);

        awaitCount(resident, 1);
        var row = indexed(resident).getFirst();
        assertThat(row).containsEntry("source_type", "CARE_TASK_RESULT");
        assertThat(row).containsEntry("source_id", Long.toString(result));
        assertThat(row).containsEntry("billing_unit", "UNIT");
        assertThat(row).containsEntry("service_id", service);
        assertThat(row.get("incurred_on").toString()).isEqualTo("2026-11-05");
        assertThat(((LocalDateTime) row.get("occurred_at")).toInstant(ZoneOffset.UTC))
                .isEqualTo(PERFORMED_AT);
        assertThat((BigDecimal) row.get("current_quantity")).isEqualByComparingTo("2.25");
        noBusinessRecords(resident);
    }

    @ParameterizedTest
    @EnumSource(BilledSource.class)
    void FR_005_medicationKeepsTheOriginalSourceAndQuantity(BilledSource type) {
        long resident = TestUsers.nextId(), source = TestUsers.nextId(), medication = TestUsers.nextId();

        publish(new MedicationBilledEvent(type, source, resident, medication, new BigDecimal("0.50"), PERFORMED_AT));

        awaitCount(resident, 1);
        var row = indexed(resident).getFirst();
        assertThat(row).containsEntry("source_type", type.name());
        assertThat(row).containsEntry("source_id", Long.toString(source));
        assertThat(row).containsEntry("billing_unit", Long.toString(medication));
        assertThat(row).containsEntry("medication_id", medication);
        assertThat(row.get("service_id")).isNull();
        assertThat(row.get("incurred_on").toString()).isEqualTo("2026-11-05");
        assertThat((BigDecimal) row.get("current_quantity")).isEqualByComparingTo("0.50");
        noBusinessRecords(resident);
    }

    @Test
    void FR_005_medicationIdentitySeparatesTwoDrugsSharingASourceReference() {
        long resident = TestUsers.nextId(), source = TestUsers.nextId();
        long first = TestUsers.nextId(), second = TestUsers.nextId();

        publish(new MedicationBilledEvent(
                BilledSource.DISPENSE_RECORD, source, resident, first, BigDecimal.ONE, PERFORMED_AT));
        publish(new MedicationBilledEvent(
                BilledSource.DISPENSE_RECORD, source, resident, second, new BigDecimal("2.00"), PERFORMED_AT));

        awaitCount(resident, 2);
        assertThat(indexed(resident))
                .extracting(row -> row.get("medication_id"))
                .containsExactlyInAnyOrder(first, second);
        assertThat(receiptCount(resident)).isEqualTo(2);
        noBusinessRecords(resident);
    }

    @Test
    void FR_005_medicationQuantityIsNotRoundedToTwoDecimals() {
        long resident = TestUsers.nextId();
        publish(new MedicationBilledEvent(
                BilledSource.DOSE,
                TestUsers.nextId(),
                resident,
                TestUsers.nextId(),
                new BigDecimal("0.125"),
                PERFORMED_AT));

        awaitCount(resident, 1);
        assertThat((BigDecimal) indexed(resident).getFirst().get("current_quantity"))
                .isEqualByComparingTo("0.125");
        noBusinessRecords(resident);
    }

    @Test
    void NFR_04_rolledBackSourceCommandDoesNotLeaveAnIndexOrReceipt() {
        long resident = TestUsers.nextId();
        var event = source("FAMILY_MEAL", resident, "rollback-" + resident, "UNIT", "rollback:" + resident);

        transaction().executeWithoutResult(status -> {
            sources.billed(event);
            status.setRollbackOnly();
        });

        await().during(Duration.ofMillis(300)).atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            assertThat(indexed(resident)).isEmpty();
            assertThat(receiptCount(resident)).isZero();
        });
        noBusinessRecords(resident);
    }
}
