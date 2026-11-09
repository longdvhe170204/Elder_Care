package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.roombedallocation.api.spi.*;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCommandStore;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedHoldService;
import com.eldercare.backend.support.TableRows;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
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
    com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationStore positions;

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

    @Test
    void FR_024_absenceWithRetentionKeepsAllocationAndHoldDeadline() {
        TableRows.each(
                Stream.of(StatusCommand.GRANT_TEMPORARY_ABSENCE, StatusCommand.TRANSFER_OUT), this::absenceKeepsBed);
    }

    /** Mỗi dòng dựng giường, người cao tuổi riêng như một ca của test tham số trước đây. */
    private void absenceKeepsBed(StatusCommand command) {
        freshFixture();
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

    /** T053 (FR-024a): trở về giường giữ ở phòng đang cách ly không bị chặn, phân bổ mang dấu chờ bác sĩ xác nhận. */
    @Test
    void FR_024a_returnToIsolatedRoomKeepsTheBedAndFlagsAwaitingDoctor() {
        freshFixture();
        long id = allocation("ACTIVE");
        residency.resident(
                resident,
                ResidencyType.LONG_TERM,
                null,
                null,
                null,
                new HeldBed(true, Optional.of(time.now().plusSeconds(3600))));
        apply(StatusCommand.GRANT_TEMPORARY_ABSENCE, ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT);
        jdbc.update("UPDATE room SET isolated=TRUE WHERE id=?", room);
        apply(StatusCommand.RECORD_RETURN, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForMap(
                        "SELECT status, awaiting_doctor_confirmation, doctor_confirmation_requested_at FROM bed_allocation WHERE id=?",
                        id))
                .containsEntry("status", "ACTIVE")
                .containsEntry("awaiting_doctor_confirmation", true)
                .doesNotContainEntry("doctor_confirmation_requested_at", null);
    }

    @Test
    void FR_025_finalStatusClosesAllocationAndReleasesBed() {
        TableRows.each(
                Stream.of(StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH), this::finalStatusReleasesBed);
    }

    private void finalStatusReleasesBed(StatusCommand command) {
        freshFixture();
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

    private void freshFixture() {
        structureFixture();
        residentFixture();
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
}
