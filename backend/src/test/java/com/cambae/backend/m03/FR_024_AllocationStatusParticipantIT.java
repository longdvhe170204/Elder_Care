package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.ResidentStatusApi;
import com.cambae.backend.m01.api.dto.*;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import com.cambae.backend.m03.api.spi.*;
import com.cambae.backend.m03.internal.allocation.persistence.AllocationCommandStore;
import com.cambae.backend.m03.internal.bed.service.BedHoldService;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;

/** T039/T043: tác động trong transaction nguồn, có kiểm rollback qua API thật của M01. */
class FR_024_AllocationStatusParticipantIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    ResidentStatusApi statusApi;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    FakeAllocationResidencySource residency;

    @Autowired
    BedHoldService holds;

    @MockitoSpyBean
    AllocationCommandStore store;

    @MockitoSpyBean
    com.cambae.backend.m03.internal.allocation.persistence.AllocationStore positions;

    long resident;

    @BeforeEach
    void residentFixture() {
        resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, new HeldBed(false, Optional.empty()));
    }

    private StatusTransitionParticipant participant() {
        assertThat(context.containsBean("allocationStatusParticipant"))
                .as("Cần participant M03 cho lệnh M01")
                .isTrue();
        return context.getBean("allocationStatusParticipant", StatusTransitionParticipant.class);
    }

    private long allocation(String status) {
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,?, 'BASIC','CURRENT')",
                resident,
                bed,
                status,
                Timestamp.from(time.now().minusSeconds(3600)));
        if (status.equals("ACTIVE")) jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        return jdbc.queryForObject("SELECT id FROM bed_allocation WHERE resident_id=?", Long.class, resident);
    }

    private void apply(StatusCommand command, ResidentStatus from, ResidentStatus to) {
        var result =
                new TransitionResult(resident, from, to, command, 1, time.now(), JsonNodeFactory.instance.objectNode());
        new TransactionTemplate(transactions)
                .executeWithoutResult(status -> participant().applyEffects(result));
    }

    @Test
    void FR_023_completingAdmissionStartsFutureAtActualCommandTime() {
        long id = allocation("FUTURE");
        apply(StatusCommand.COMPLETE_ADMISSION, ResidentStatus.ADMISSION_IN_PROGRESS, ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
    }

    @ParameterizedTest
    @EnumSource(
            value = StatusCommand.class,
            names = {"GRANT_TEMPORARY_ABSENCE", "TRANSFER_OUT"})
    void FR_024_absenceWithRetentionKeepsAllocationAndHoldDeadline(StatusCommand command) {
        allocation("ACTIVE");
        residency.resident(
                resident,
                ResidencyType.LONG_TERM,
                null,
                null,
                null,
                new HeldBed(true, Optional.of(time.now().plusSeconds(3600))));
        apply(
                command,
                ResidentStatus.IN_RESIDENCE,
                command == StatusCommand.TRANSFER_OUT
                        ? ResidentStatus.HOSPITALIZED
                        : ResidentStatus.TEMPORARILY_ABSENT);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("HELD");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT hold_until FROM bed WHERE id=?", Timestamp.class, bed))
                .isNotNull();
    }

    @Test
    void FR_024_absenceWithoutRetentionClosesAndAwaitsReadiness() {
        allocation("ACTIVE");
        apply(StatusCommand.GRANT_TEMPORARY_ABSENCE, ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("CLOSED");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
    }

    @Test
    void FR_025a_returnWithoutAllocationMarksResidentWithoutBed() {
        apply(StatusCommand.RECORD_RETURN, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject(
                        "SELECT without_bed_since FROM resident_position WHERE resident_id=?",
                        Timestamp.class,
                        resident))
                .isNotNull();
    }

    @Test
    void FR_024a_returnToIsolatedRoomIsAllowedAndFlagged() {
        allocation("ACTIVE");
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=? WHERE id=?",
                resident,
                bed);
        jdbc.update("UPDATE room SET isolated=TRUE WHERE id=?", room);
        apply(StatusCommand.RECORD_RETURN, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForObject(
                        "SELECT awaiting_doctor_confirmation FROM bed_allocation WHERE resident_id=?",
                        Boolean.class,
                        resident))
                .isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = StatusCommand.class,
            names = {"END_RESIDENCY", "RECORD_DEATH"})
    void FR_025_finalStatusClosesAllocationAndReleasesBed(StatusCommand command) {
        allocation("ACTIVE");
        apply(
                command,
                ResidentStatus.IN_RESIDENCE,
                command == StatusCommand.RECORD_DEATH ? ResidentStatus.DECEASED : ResidentStatus.RESIDENCY_ENDED);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("CLOSED");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
    }

    @Test
    void FR_047_cancelAdmissionFailureRollsBackM01AndM03Together() {
        participant();
        long id = allocation("FUTURE");
        doThrow(new IllegalStateException("Lỗi M03 sau đổi trạng thái M01"))
                .when(store)
                .cancel(anyLong(), anyString());
        assertThatThrownBy(() -> new TransactionTemplate(transactions)
                        .executeWithoutResult(status -> statusApi.execute(
                                resident,
                                StatusCommand.CANCEL_ADMISSION,
                                user,
                                Optional.of(Reason.of("Hủy tiếp nhận")),
                                JsonNodeFactory.instance.objectNode(),
                                new Basis(null, null, 1L, null))))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lỗi M03");
        assertThat(jdbc.queryForObject("SELECT status FROM resident WHERE id=?", String.class, resident))
                .isEqualTo("ADMISSION_IN_PROGRESS");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id))
                .isEqualTo("FUTURE");
    }

    @Test
    void FR_025_cancelAdmissionUsesM01TransactionAndAuditsCancellation() {
        participant();
        long id = allocation("FUTURE");
        new TransactionTemplate(transactions)
                .executeWithoutResult(status -> statusApi.execute(
                        resident,
                        StatusCommand.CANCEL_ADMISSION,
                        user,
                        Optional.of(Reason.of("Hủy tiếp nhận")),
                        JsonNodeFactory.instance.objectNode(),
                        new Basis(null, null, 1L, null)));
        assertThat(jdbc.queryForObject("SELECT status FROM resident WHERE id=?", String.class, resident))
                .isEqualTo("ADMISSION_CANCELLED");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M03.BED_ALLOCATION' AND entity_id=? AND action='M03_ALLOCATION_CANCELLED'",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void FR_024_transferOutWhileAlreadyAbsentHandlesRetainedBed(boolean keep) {
        allocation("ACTIVE");
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=? WHERE id=?",
                resident,
                bed);
        var deadline = time.now().plusSeconds(7200).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        residency.resident(
                resident, ResidencyType.LONG_TERM, null, null, null, new HeldBed(keep, Optional.of(deadline)));
        apply(StatusCommand.TRANSFER_OUT, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.HOSPITALIZED);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo(keep ? "HELD" : "AWAITING_READY");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo(keep ? "ACTIVE" : "CLOSED");
        if (keep)
            assertThat(jdbc.queryForObject("SELECT hold_until FROM bed WHERE id=?", Timestamp.class, bed)
                            .toInstant())
                    .isEqualTo(deadline);
    }

    @Test
    void FR_025a_dayCareReturnDoesNotCreateWithoutBedMarker() {
        residency.resident(resident, ResidencyType.DAY_CARE, null, null, null, new HeldBed(false, Optional.empty()));
        apply(StatusCommand.RECORD_RETURN, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject(
                        "SELECT without_bed_since FROM resident_position WHERE resident_id=?",
                        Timestamp.class,
                        resident))
                .isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_type='RESIDENT' AND target_id=?",
                        Integer.class,
                        resident))
                .isZero();
    }

    @Test
    void FR_023_admissionRechecksHoldCommittedAfterSourceSnapshot() {
        long id = allocation("FUTURE");
        jdbc.update(
                "UPDATE bed_allocation SET start_at=? WHERE id=?",
                Timestamp.from(time.now().plusSeconds(90L * 86400)),
                id);
        var source = new TransactionTemplate(transactions);
        source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThatThrownBy(() -> source.executeWithoutResult(tx -> {
                    assertThat(participant()
                                    .checkConditions(new CommandContext(
                                            resident,
                                            ResidentStatus.ADMISSION_IN_PROGRESS,
                                            StatusCommand.COMPLETE_ADMISSION,
                                            user,
                                            time.now(),
                                            JsonNodeFactory.instance.objectNode())))
                            .isEmpty();
                    holdInOtherTransaction(TestUsers.nextId());
                    participant()
                            .applyEffects(new TransitionResult(
                                    resident,
                                    ResidentStatus.ADMISSION_IN_PROGRESS,
                                    ResidentStatus.IN_RESIDENCE,
                                    StatusCommand.COMPLETE_ADMISSION,
                                    1,
                                    time.now(),
                                    JsonNodeFactory.instance.objectNode()));
                }))
                .isInstanceOf(com.cambae.backend.common.error.ConditionNotMetException.class);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("HELD");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id))
                .isEqualTo("FUTURE");
    }

    @Test
    void FR_025_cancelAdmissionReleasesHoldCommittedAfterSourceSnapshot() {
        var source = new TransactionTemplate(transactions);
        source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        source.executeWithoutResult(tx -> {
            jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed);
            holdInOtherTransaction(resident);
            participant()
                    .applyEffects(new TransitionResult(
                            resident,
                            ResidentStatus.ADMISSION_IN_PROGRESS,
                            ResidentStatus.ADMISSION_CANCELLED,
                            StatusCommand.CANCEL_ADMISSION,
                            1,
                            time.now(),
                            JsonNodeFactory.instance.objectNode()));
        });
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("EMPTY");
    }

    @Test
    void FR_020_admissionAndRoomEditUseCompatibleLockOrder() throws Exception {
        allocation("FUTURE");
        var bedLocked = new java.util.concurrent.CountDownLatch(1);
        var roomLocked = new java.util.concurrent.CountDownLatch(1);
        org.mockito.Mockito.doAnswer(call -> {
                    call.callRealMethod();
                    bedLocked.countDown();
                    roomLocked.await(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                    return null;
                })
                .when(positions)
                .lockBed(bed);
        try (var threads = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var admission = threads.submit(() -> apply(
                    StatusCommand.COMPLETE_ADMISSION,
                    ResidentStatus.ADMISSION_IN_PROGRESS,
                    ResidentStatus.IN_RESIDENCE));
            var edit = threads.submit(() -> {
                assertThat(bedLocked.await(5, java.util.concurrent.TimeUnit.SECONDS))
                        .isTrue();
                new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                    jdbc.queryForList("SELECT id FROM room WHERE id=? FOR UPDATE", Long.class, room);
                    roomLocked.countDown();
                    jdbc.queryForList("SELECT id FROM bed WHERE room_id=? ORDER BY id FOR UPDATE", Long.class, room);
                    jdbc.update("UPDATE room SET name='Phòng đã sửa tên' WHERE id=?", room);
                });
                return true;
            });
            admission.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(edit.get(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
    }

    private void holdInOtherTransaction(long owner) {
        var other = new TransactionTemplate(transactions);
        other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        other.executeWithoutResult(tx -> holds.holdForWaitingRecord(TestUsers.nextId(), owner, bed, time.now()));
    }

    @Test
    void FR_020_admissionsCanReadWhenAllMainPoolConnectionsAreBusy() throws Exception {
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 5; i++) {
            long person = TestUsers.nextId(), target = TestUsers.nextId();
            ids.add(person);
            jdbc.update(
                    "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                    person,
                    Timestamp.from(time.now()),
                    Timestamp.from(time.now()),
                    user.accountId());
            jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,?)", target, room, "G" + (i + 2));
            jdbc.update(
                    "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                    person,
                    target,
                    Timestamp.from(time.now().minusSeconds(3600)));
            residency.resident(person, ResidencyType.LONG_TERM, null, null, null, new HeldBed(false, Optional.empty()));
        }
        assertThat(context.getBean(javax.sql.DataSource.class)
                        .unwrap(com.zaxxer.hikari.HikariDataSource.class)
                        .getMaximumPoolSize())
                .isEqualTo(5);
        var occupied = new java.util.concurrent.CountDownLatch(5);
        try (var threads = java.util.concurrent.Executors.newFixedThreadPool(5)) {
            var results = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (long person : ids)
                results.add(threads.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                    jdbc.queryForObject("SELECT 1", Integer.class);
                    occupied.countDown();
                    try {
                        assertThat(occupied.await(5, java.util.concurrent.TimeUnit.SECONDS))
                                .isTrue();
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(error);
                    }
                    participant()
                            .applyEffects(new TransitionResult(
                                    person,
                                    ResidentStatus.ADMISSION_IN_PROGRESS,
                                    ResidentStatus.IN_RESIDENCE,
                                    StatusCommand.COMPLETE_ADMISSION,
                                    person,
                                    time.now(),
                                    JsonNodeFactory.instance.objectNode()));
                })));
            for (var result : results) result.get(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation a JOIN bed b ON b.id=a.bed_id WHERE b.room_id=? AND a.status='ACTIVE'",
                        Integer.class,
                        room))
                .isEqualTo(5);
    }

    @Test
    void FR_019_admissionSeesLatestCorrectionCommittedAfterSourceSnapshot() {
        long previous = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,?,'CLOSED',?,?,'BASIC','CURRENT')",
                previous,
                TestUsers.nextId(),
                bed,
                Timestamp.from(time.now().minusSeconds(10800)),
                Timestamp.from(time.now().minusSeconds(7200)));
        String correction =
                "INSERT INTO correction(record_type,original_record_id,type,corrected_content,reason,actor_id,creation_method,recorded_at,sequence_no) VALUES('M03.BED_ALLOCATION',?,'AMEND_CONTENT',?,'Đính chính trong test',?,'A',?,?)";
        jdbc.update(
                correction,
                previous,
                "{\"endAt\":\"" + time.now().minusSeconds(7200) + "\"}",
                user.accountId(),
                Timestamp.from(time.now()),
                1);
        allocation("FUTURE");
        var source = new TransactionTemplate(transactions);
        source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThatThrownBy(() -> source.executeWithoutResult(tx -> {
                    assertThat(participant()
                                    .checkConditions(new CommandContext(
                                            resident,
                                            ResidentStatus.ADMISSION_IN_PROGRESS,
                                            StatusCommand.COMPLETE_ADMISSION,
                                            user,
                                            time.now(),
                                            JsonNodeFactory.instance.objectNode())))
                            .isEmpty();
                    var other = new TransactionTemplate(transactions);
                    other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    other.executeWithoutResult(inner -> jdbc.update(
                            correction,
                            previous,
                            "{\"endAt\":\"" + time.now().plusSeconds(7200) + "\"}",
                            user.accountId(),
                            Timestamp.from(time.now()),
                            2));
                    participant()
                            .applyEffects(new TransitionResult(
                                    resident,
                                    ResidentStatus.ADMISSION_IN_PROGRESS,
                                    ResidentStatus.IN_RESIDENCE,
                                    StatusCommand.COMPLETE_ADMISSION,
                                    1,
                                    time.now(),
                                    JsonNodeFactory.instance.objectNode()));
                }))
                .isInstanceOf(com.cambae.backend.common.error.ConditionNotMetException.class);
    }

    @Test
    void FR_020_twoAdmissionsInSameRoomDoNotUpgradeRoomLock() throws Exception {
        allocation("FUTURE");
        long nextResident = TestUsers.nextId(), nextBed = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 2]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                nextResident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", nextBed, room);
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                nextResident,
                nextBed,
                Timestamp.from(time.now().minusSeconds(3600)));
        residency.resident(
                nextResident, ResidencyType.LONG_TERM, null, null, null, new HeldBed(false, Optional.empty()));
        var ready = new java.util.concurrent.CountDownLatch(2);
        try (var threads = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = threads.submit(() -> admissionTogether(resident, null, ready));
            var second = threads.submit(() -> admissionTogether(nextResident, null, ready));
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id IN (?,?) AND status='ACTIVE'",
                        Integer.class,
                        resident,
                        nextResident))
                .isEqualTo(2);
    }

    private void admissionTogether(long person, Long replacement, java.util.concurrent.CountDownLatch ready) {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            jdbc.queryForObject("SELECT 1", Integer.class);
            ready.countDown();
            try {
                assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS))
                        .isTrue();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
            var details = JsonNodeFactory.instance.objectNode();
            if (replacement != null) details.put("switchAllocationToBedId", replacement);
            participant()
                    .applyEffects(new TransitionResult(
                            person,
                            ResidentStatus.ADMISSION_IN_PROGRESS,
                            ResidentStatus.IN_RESIDENCE,
                            StatusCommand.COMPLETE_ADMISSION,
                            person,
                            time.now(),
                            details));
        });
    }

    @Test
    void FR_020_parallelAdmissionReplacementsIntoUnusedBedsDoNotDeadlockOnSupremumGap() throws Exception {
        allocation("FUTURE");
        long nextResident = TestUsers.nextId(), nextBed = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 2]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                nextResident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", nextBed, room);
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                nextResident,
                nextBed,
                Timestamp.from(time.now().minusSeconds(3600)));
        residency.resident(
                nextResident, ResidencyType.LONG_TERM, null, null, null, new HeldBed(false, Optional.empty()));
        long
                target1 =
                        Math.max(
                                jdbc.queryForObject("SELECT MAX(bed_id)+1 FROM bed_allocation", Long.class),
                                1_000_000_000_000L + TestUsers.nextId() * 2),
                target2 = target1 + 1;
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G3'),(?,?,'G4')", target1, room, target2, room);
        var ready = new java.util.concurrent.CountDownLatch(2);
        try (var threads = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = threads.submit(() -> admissionTogether(resident, target1, ready));
            var second = threads.submit(() -> admissionTogether(nextResident, target2, ready));
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE bed_id IN (?,?) AND status='ACTIVE'",
                        Integer.class,
                        target1,
                        target2))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id IN (?,?) AND status='CANCELLED'",
                        Integer.class,
                        resident,
                        nextResident))
                .isEqualTo(2);
    }

    @Test
    void FR_024a_returnUsesIsolationCommittedAfterSourceSnapshot() {
        allocation("ACTIVE");
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=? WHERE id=?",
                resident,
                bed);
        var source = new TransactionTemplate(transactions);
        source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        source.executeWithoutResult(tx -> {
            jdbc.queryForObject("SELECT isolated FROM room WHERE id=?", Boolean.class, room);
            var other = new TransactionTemplate(transactions);
            other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            other.executeWithoutResult(inner -> jdbc.update("UPDATE room SET isolated=TRUE WHERE id=?", room));
            participant()
                    .applyEffects(new TransitionResult(
                            resident,
                            ResidentStatus.TEMPORARILY_ABSENT,
                            ResidentStatus.IN_RESIDENCE,
                            StatusCommand.RECORD_RETURN,
                            1,
                            time.now(),
                            JsonNodeFactory.instance.objectNode()));
        });
        assertThat(jdbc.queryForObject(
                        "SELECT awaiting_doctor_confirmation FROM bed_allocation WHERE resident_id=?",
                        Boolean.class,
                        resident))
                .isTrue();
    }

    @Test
    void FR_023_admissionRechecksRoomPolicyCommittedAfterSourceSnapshot() {
        allocation("FUTURE");
        var source = new TransactionTemplate(transactions);
        source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThatThrownBy(() -> source.executeWithoutResult(tx -> {
                    assertThat(participant()
                                    .checkConditions(new CommandContext(
                                            resident,
                                            ResidentStatus.ADMISSION_IN_PROGRESS,
                                            StatusCommand.COMPLETE_ADMISSION,
                                            user,
                                            time.now(),
                                            JsonNodeFactory.instance.objectNode())))
                            .isEmpty();
                    var other = new TransactionTemplate(transactions);
                    other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    other.executeWithoutResult(
                            inner -> jdbc.update("UPDATE room SET gender_policy='MALE' WHERE id=?", room));
                    participant()
                            .applyEffects(new TransitionResult(
                                    resident,
                                    ResidentStatus.ADMISSION_IN_PROGRESS,
                                    ResidentStatus.IN_RESIDENCE,
                                    StatusCommand.COMPLETE_ADMISSION,
                                    1,
                                    time.now(),
                                    JsonNodeFactory.instance.objectNode()));
                }))
                .isInstanceOf(com.cambae.backend.common.error.ConditionNotMetException.class);
    }
}
