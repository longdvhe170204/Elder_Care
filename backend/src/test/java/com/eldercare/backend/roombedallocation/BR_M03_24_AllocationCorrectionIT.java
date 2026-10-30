package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.events.BedAllocationCorrected;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.*;
import org.springframework.transaction.annotation.Transactional;

/** T047: dùng cơ chế đính chính thật, rollback toàn bộ khi FR-019 không thỏa. */
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
@RecordApplicationEvents
class BR_M03_24_AllocationCorrectionIT extends StructureTestSupport {
    @Autowired
    CorrectionApi corrections;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.administration.account.api.PermissionApi permissions;

    @Autowired
    List<CorrectableRecordProvider> providers;

    @Autowired
    ApplicationEvents events;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    long id, resident;

    @BeforeEach
    void correctionFixture() {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        id = allocate(resident, bed, start, start.plusSeconds(3600));
    }

    @Test
    void FR_026_revokedRecordPermissionCannotUseOriginalRecorderMethodA() {
        org.mockito.Mockito.doReturn(com.eldercare.backend.administration.account.api.dto.PermissionResult.denied(
                        com.eldercare.backend.administration.account.api.dto.FailedLayer.ROLE))
                .when(permissions)
                .hasPermission(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        assertThatThrownBy(() -> corrections.create(
                        "M03.BED_ALLOCATION",
                        id,
                        CorrectionType.AMEND_CONTENT,
                        Map.of("endAt", start.plusSeconds(7200).toString()),
                        new Reason("Sửa mốc")))
                .isInstanceOfSatisfying(
                        com.eldercare.backend.common.error.BusinessException.class,
                        error -> assertThat(error.status().value()).isEqualTo(403));
    }

    private long allocate(long who, long target, Instant from, Instant to) {
        long allocation = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source,opened_by_id) VALUES(?,?,?,'CLOSED',?,?,'BASIC','CURRENT',?)",
                allocation,
                who,
                target,
                Timestamp.from(from),
                Timestamp.from(to),
                user.accountId());
        return allocation;
    }

    private CorrectableRecordProvider provider(String type) {
        return providers.stream()
                .filter(p -> p.recordType().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu provider " + type));
    }

    @Test
    void FR_049_correctionAuditKeepsResidentOfOriginalAllocation() {
        corrections.create(
                "M03.BED_ALLOCATION",
                id,
                CorrectionType.AMEND_CONTENT,
                Map.of("endAt", start.plusSeconds(7200).toString()),
                new Reason("Sửa mốc kết thúc"));
        assertThat(jdbc.queryForObject(
                        "SELECT resident_id FROM audit_log WHERE action='CORRECTION_ADDED'"
                                + " AND entity_type='M03.BED_ALLOCATION' AND entity_id=?",
                        Long.class,
                        Long.toString(id)))
                .isEqualTo(resident);
    }

    @Test
    void BR_M03_24_amendChangesCurrentViewAndPublishesBeforeAfter() {
        assertThat(provider("M03.BED_ALLOCATION").floorArea(id)).contains(floor);
        var saved = corrections.create(
                "M03.BED_ALLOCATION",
                id,
                CorrectionType.AMEND_CONTENT,
                Map.of("endAt", start.plusSeconds(7200).toString()),
                new Reason("Sửa mốc kết thúc"));
        assertThat(saved).isInstanceOf(CorrectionResult.Saved.class);
        assertThat(jdbc.queryForObject("SELECT end_at FROM v_current_allocation WHERE id=?", Timestamp.class, id)
                        .toInstant())
                .isEqualTo(start.plusSeconds(7200));
        assertThat(events.stream(BedAllocationCorrected.class)).singleElement().satisfies(event -> {
            assertThat(event.allocationId()).isEqualTo(id);
            assertThat(event.before().get("endAt"))
                    .isEqualTo(start.plusSeconds(3600).toString());
            assertThat(event.after().get("endAt"))
                    .isEqualTo(start.plusSeconds(7200).toString());
        });
        assertThat(jdbc.queryForObject("SELECT end_at FROM bed_allocation WHERE id=?", Timestamp.class, id)
                        .toInstant())
                .isEqualTo(start.plusSeconds(3600));
    }

    @Test
    void FR_019_correctionCannotOverlapAnotherAllocation() {
        allocate(TestUsers.nextId(), bed, start.plusSeconds(3600), start.plusSeconds(10800));
        assertThatThrownBy(() -> corrections.create(
                        "M03.BED_ALLOCATION",
                        id,
                        CorrectionType.AMEND_CONTENT,
                        Map.of("endAt", start.plusSeconds(7200).toString()),
                        new Reason("Sửa mốc")))
                .isInstanceOfSatisfying(
                        com.eldercare.backend.common.error.BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo("ALLOCATION_TIME_OVERLAP"));
        assertThat(events.stream(BedAllocationCorrected.class)).isEmpty();
    }

    @Test
    void FR_019_sameResidentCannotOccupyTwoBedsAfterCorrection() {
        long otherBed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", otherBed, room);
        allocate(resident, otherBed, start.plusSeconds(3600), start.plusSeconds(10800));
        assertThatThrownBy(() -> corrections.create(
                        "M03.BED_ALLOCATION",
                        id,
                        CorrectionType.AMEND_CONTENT,
                        Map.of("endAt", start.plusSeconds(7200).toString()),
                        new Reason("Sửa mốc")))
                .isInstanceOfSatisfying(
                        com.eldercare.backend.common.error.BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo("ALLOCATION_TIME_OVERLAP"));
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void BR_M03_24_rrSnapshotDoesNotLosePreviousCommittedCorrectionInEvent() {
        var outer = new org.springframework.transaction.support.TransactionTemplate(transactions);
        outer.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var fresh = new org.springframework.transaction.support.TransactionTemplate(transactions);
        fresh.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        fresh.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        outer.executeWithoutResult(status -> {
            jdbc.queryForObject(
                    "SELECT COUNT(*) FROM correction WHERE record_type='M03.BED_ALLOCATION' AND original_record_id=?",
                    Long.class,
                    id);
            fresh.executeWithoutResult(inner -> corrections.create(
                    "M03.BED_ALLOCATION",
                    id,
                    CorrectionType.AMEND_CONTENT,
                    Map.of("endAt", start.plusSeconds(7200).toString()),
                    new Reason("Lần trước")));
            corrections.create(
                    "M03.BED_ALLOCATION",
                    id,
                    CorrectionType.AMEND_CONTENT,
                    Map.of("endAt", start.plusSeconds(9000).toString()),
                    new Reason("Lần sau"));
        });
        var published = events.stream(BedAllocationCorrected.class).toList();
        assertThat(published).hasSize(2);
        assertThat(published.getLast().before().get("endAt"))
                .isEqualTo(start.plusSeconds(7200).toString());
    }

    @Test
    void FR_026_genericCorrectionPayloadRequiresCanonicalUtcMicroseconds() {
        var provider = provider("M03.BED_ALLOCATION");
        assertThatThrownBy(() -> provider.validateContent(Map.of("endAt", "2026-01-01T09:00:00+07:00")))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
        assertThatThrownBy(() -> provider.validateContent(Map.of("endAt", "2026-01-01T02:00:00.123456789Z")))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
    }

    @Test
    void FR_026_onlyClosedOrCancelledAndWhitelistedContent() {
        var provider = provider("M03.BED_ALLOCATION");
        assertThatThrownBy(() -> provider.validateContent(Map.of("residentId", 42)))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
        assertThatThrownBy(() -> provider.validateContent(Map.of("bedId", 1.5)))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
        assertThat(provider.isConfirmed(id)).isTrue();
    }

    @Test
    void FR_044_readinessProviderOnlyAcceptsNoteAndDisinfectionFacts() {
        var provider = provider("M03.BED_READINESS_CONFIRMATION");
        assertThatThrownBy(() -> provider.validateContent(Map.of("toState", "EMPTY")))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
        assertThatThrownBy(() -> provider.validateContent(Map.of("disinfected", "yes")))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
        provider.validateContent(Map.of("note", "Ghi đúng", "disinfected", true, "ppeUsed", true));
    }
}
