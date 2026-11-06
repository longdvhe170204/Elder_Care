package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.Legacy;
import com.eldercare.backend.staffing.internal.domain.StaffingEventData;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** FR-037/R-15: kiểu nội dung qua API thật, bất biến tại DB và lưu giữ giữ định danh logic. */
class StaffingRatioSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecordEventApi events;

    @Autowired
    List<RetentionProvider> providers;

    @Autowired
    PlatformTransactionManager manager;

    @Autowired
    RetentionDatabase database;

    private RetentionProvider provider() {
        return providers.stream()
                .filter(p -> p.dataType().equals("M09.STAFFING_RATIO_CALCULATION"))
                .findFirst()
                .orElseThrow();
    }

    private RecordEvent<StaffingEventData.Ratio> add(long shift, StaffingEventData.Ratio value, String key) {
        return new TransactionTemplate(manager)
                .execute(status -> events.add(
                        StaffingEventData.RATIO,
                        new EntityRef("M09.SHIFT", shift),
                        new NewRecordEvent<>(
                                value,
                                null,
                                null,
                                false,
                                value.calculatedAt(),
                                null,
                                value.calculatedAt(),
                                null,
                                key)));
    }

    private StaffingEventData.Ratio value(Instant at, String event, Legacy legacy) {
        return new StaffingEventData.Ratio(at, event, BigDecimal.ONE, 1, BigDecimal.ONE, BigDecimal.ONE, true, legacy);
    }

    @Test
    void FR_037_calculationHasRetentionProvider() {
        assertThat(providers).anyMatch(p -> p.dataType().equals("M09.STAFFING_RATIO_CALCULATION"));
    }

    @Test
    void FR_037_calculationsAppendOnly_withTenTriggerTypesAndDecimalWeight() {
        long shift = TestUsers.nextId();
        var at = Instant.parse("2026-10-25T00:00:00Z");
        var saved = add(
                shift,
                new StaffingEventData.Ratio(
                        at,
                        "DRAFT_SAVED",
                        new BigDecimal("35.00"),
                        4,
                        new BigDecimal("8.75"),
                        new BigDecimal("8.00"),
                        false,
                        null),
                null);
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(content->'$.totalWeight' AS DECIMAL(8,2)) FROM record_event WHERE id=?",
                        BigDecimal.class,
                        saved.id()))
                .isEqualByComparingTo("35.00");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE record_event SET content=JSON_SET(content,'$.staffCount',8) WHERE id=?", saved.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM record_event WHERE id=?", saved.id()))
                .isInstanceOf(DataAccessException.class);
        for (String event : List.of(
                "DRAFT_SAVED",
                "PUBLISHED",
                "STAFF_ABSENT",
                "STAFF_ADDED",
                "EMPLOYMENT_ENDED",
                "SHIFT_SWAP_015",
                "CARE_LEVEL_CHANGED",
                "ALLOCATION_CHANGED",
                "RESIDENT_ABSENCE",
                "FINAL_STATUS")) {
            assertThat(add(shift, value(at, event, null), null).content().event())
                    .isEqualTo(event);
        }
        assertThatThrownBy(() -> add(shift, value(at, "UNKNOWN", null), null))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_037_retentionFindsExpiredRatioEvenWhenDraftShiftHasBeenDeleted() {
        var at = Instant.parse("2000-01-01T00:00:00Z");
        long deletedShift = TestUsers.nextId();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM work_shift WHERE id=?", Integer.class, deletedShift))
                .isZero();
        var event = add(deletedShift, value(at, "DRAFT_SAVED", null), null);
        assertThat(provider().expiredCandidates(at.plusSeconds(1), 100))
                .as("FR_037: phải tìm được lịch sử tỷ lệ hết hạn kể cả khi ca Nháp đã bị xóa")
                .anyMatch(candidate -> candidate.id() == event.id());
        assertThat(provider().expiredCandidates(at, 100)).noneMatch(candidate -> candidate.id() == event.id());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void FR_037_retentionFindsOrphansWithNativeAndMigratedLogicalIds(boolean migrated) {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var at = Instant.parse("1000-01-01T00:00:00Z");
            long oldId = TestUsers.nextId();
            var event = add(
                    TestUsers.nextId(),
                    value(at, "DRAFT_SAVED", migrated ? new Legacy("staffing_ratio_calculation", oldId) : null),
                    migrated ? "M09.STAFFING_RATIO_CALCULATION:legacy:" + oldId : null);
            long logicalId = migrated ? oldId : event.id();

            assertThat(provider().expiredCandidates(at.plusSeconds(1), 1))
                    .singleElement()
                    .satisfies(candidate -> {
                        assertThat(candidate.id()).isEqualTo(logicalId);
                        assertThat(candidate.at()).isEqualTo(at);
                    });
            assertThat(provider().expiredCandidates(at, 1)).isEmpty();
            status.setRollbackOnly();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PUBLISHED", "IN_PROGRESS", "AWAITING_HANDOVER", "CLOSED", "CANCELLED"})
    void FR_037_retentionFiltersLiveShiftsBeforeApplyingTheBatchLimit(String shiftStatus) {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var at = Instant.parse("1000-01-01T00:00:00Z");
            long shift = shift(shiftStatus);
            var withParent = add(shift, value(at, "DRAFT_SAVED", null), null);
            var orphan = add(TestUsers.nextId(), value(at.plusSeconds(1), "DRAFT_SAVED", null), null);
            // Cùng bảng chung nhưng khác loại: không được chiếm chỗ trong batch tỷ lệ.
            events.add(
                    StaffingEventData.PUBLICATION,
                    new EntityRef("M09.SCHEDULE", TestUsers.nextId()),
                    new NewRecordEvent<>(
                            new StaffingEventData.Publication(Map.of(), null),
                            null,
                            null,
                            false,
                            at,
                            null,
                            at,
                            null,
                            null));
            long expected = Set.of("CLOSED", "CANCELLED").contains(shiftStatus) ? withParent.id() : orphan.id();

            assertThat(provider().expiredCandidates(at.plusSeconds(2), 1))
                    .singleElement()
                    .satisfies(candidate -> assertThat(candidate.id()).isEqualTo(expected));
            assertThat(provider().expiredCandidates(at.plusSeconds(2), 10))
                    .allSatisfy(candidate -> assertThat(candidate.id()).isIn(withParent.id(), orphan.id()));
            status.setRollbackOnly();
        });
    }

    @Test
    void FR_051_publicationRetentionAlsoFindsAnExpiredRecordWithoutItsSchedule() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            var at = Instant.parse("1000-01-01T00:00:00Z");
            var event = events.add(
                    StaffingEventData.PUBLICATION,
                    new EntityRef("M09.SCHEDULE", TestUsers.nextId()),
                    new NewRecordEvent<>(
                            new StaffingEventData.Publication(Map.of(), null),
                            null,
                            null,
                            false,
                            at,
                            null,
                            at,
                            null,
                            null));
            var publications = providers.stream()
                    .filter(p -> p.dataType().equals("M09.SHIFT_PUBLICATION"))
                    .findFirst()
                    .orElseThrow();

            assertThat(publications.expiredCandidates(at.plusSeconds(1), 1))
                    .singleElement()
                    .satisfies(candidate -> assertThat(candidate.id()).isEqualTo(event.id()));
            assertThat(publications.expiredCandidates(at, 1)).isEmpty();
            status.setRollbackOnly();
        });
    }

    private long shift(String state) {
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES ('M09.SHIFT_TEMPLATE','M09',?,'ACTIVE',CAST(? AS JSON))",
                "[Mẫu lưu giữ tỷ lệ] #" + TestUsers.nextId(),
                "{\"startTime\":\"07:00:00\",\"endTime\":\"15:00:00\",\"scopeType\":\"FLOOR\",\"requiresHandover\":false,\"createdAt\":\"1000-01-01T00:00:00Z\"}");
        long floor = TestUsers.nextId();
        long schedule = AssignmentFixtures.insert(
                jdbc, "INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('1000-01','FLOOR',?)", floor);
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status) VALUES (?,?,'FLOOR',?,'1000-01-01 07:00:00','1000-01-01 15:00:00',FALSE,?)",
                schedule,
                template,
                floor,
                state);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void FR_037_retentionDeletesKnownOrphanUsingItsLogicalId(boolean migrated) {
        var at = Instant.parse("2000-01-01T00:00:00Z");
        long oldId = TestUsers.nextId();
        Legacy legacy = migrated ? new Legacy("staffing_ratio_calculation", oldId) : null;
        var event = add(
                TestUsers.nextId(),
                value(at, "DRAFT_SAVED", legacy),
                migrated ? "M09.STAFFING_RATIO_CALCULATION:legacy:" + oldId : null);
        long logicalId = migrated ? oldId : event.id();
        database.transaction().executeWithoutResult(status -> provider().delete(List.of(logicalId)));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M09.STAFFING_RATIO_CALCULATION' AND id=?",
                        Integer.class,
                        event.id()))
                .isZero();
    }
}
