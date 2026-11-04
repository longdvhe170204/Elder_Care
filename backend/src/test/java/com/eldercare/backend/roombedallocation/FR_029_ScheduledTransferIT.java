package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.ScheduledTransferResult;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.roombedallocation.api.events.ScheduledTransferWaitingForBed;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T033: facade thật, khóa và hoàn tác trong transaction của module gọi. */
@RecordApplicationEvents
class FR_029_ScheduledTransferIT extends StructureTestSupport {
    @Autowired
    ObjectProvider<BedAllocationCommandApi> commands;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    FakeAllocationResidencySource residency;

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationEvents events;

    @Autowired
    com.eldercare.backend.roombedallocation.internal.bed.service.BedReadinessService readiness;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeAssignments assignments;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeDuties duties;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeBedAssets assets;

    long resident, old, target, request;
    LocalDate date;

    @BeforeEach
    void fixture() {
        // Sau lúc tạo người dùng của lớp nền, đặt giờ sáng tương lai để hạn sẵn sàng trước ngày chuyển.
        clock.setTo(
                time.today().plusDays(1).atTime(8, 0).atZone(BusinessTime.ZONE).toInstant());
        resident = TestUsers.nextId();
        old = TestUsers.nextId();
        target = TestUsers.nextId();
        request = TestUsers.nextId();
        date = time.today().plusDays(1);
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[NCT chuyển theo lịch]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now().minusSeconds(60)),
                Timestamp.from(time.now().minusSeconds(60)),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'L2')", target, room);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT')",
                old,
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(60)));
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    BedAllocationCommandApi api() {
        assertThat(commands.getIfAvailable())
                .as("T033 phải cung cấp facade production")
                .isNotNull();
        return commands.getObject();
    }

    <T> T tx(java.util.function.Supplier<T> action) {
        return new TransactionTemplate(transactions).execute(status -> action.get());
    }

    long reserve() {
        return tx(() -> api().reserveScheduledTransfer(request, resident, target, date));
    }

    Instant planned() {
        return date.atStartOfDay(BusinessTime.ZONE).toInstant();
    }

    String state(long id) {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id);
    }

    @Test
    void FR_029_reservesOnceWithoutMovingResidentOrChangingBedState() {
        long next = reserve();
        assertThat(reserve()).isEqualTo(next);
        assertThat(jdbc.queryForMap(
                        "SELECT status,basis_type,basis_id,previous_allocation_id FROM bed_allocation WHERE id=?",
                        next))
                .containsEntry("status", "FUTURE")
                .containsEntry("basis_type", "REQUEST")
                .containsEntry("basis_id", request)
                .containsEntry("previous_allocation_id", old);
        assertThat(state(bed)).isEqualTo("OCCUPIED");
        assertThat(state(target)).isEqualTo("EMPTY");
        assertThat(events.stream(BedTransferred.class).filter(e -> e.residentId() == resident))
                .isEmpty();
    }

    @Test
    void FR_029_appliesAtomicallyAndRetryDoesNotTransferTwice() {
        long next = reserve();
        clock.advanceBy(Duration.ofDays(1));
        assertThat(tx(() -> api().checkScheduledTransfer(request))).isEmpty();
        assertThat(tx(() -> api().applyScheduledTransfer(request, planned())))
                .isEqualTo(ScheduledTransferResult.TRANSFERRED);
        assertThat(tx(() -> api().applyScheduledTransfer(request, planned())))
                .isEqualTo(ScheduledTransferResult.TRANSFERRED);
        assertThat(jdbc.queryForObject("SELECT end_at FROM bed_allocation WHERE id=?", Timestamp.class, old))
                .isEqualTo(
                        jdbc.queryForObject("SELECT start_at FROM bed_allocation WHERE id=?", Timestamp.class, next));
        assertThat(state(bed)).isEqualTo("AWAITING_READY");
        assertThat(state(target)).isEqualTo("OCCUPIED");
        assertThat(events.stream(BedTransferred.class).filter(e -> e.residentId() == resident))
                .hasSize(1);
        assertThat(jdbc.queryForObject("SELECT executed_late FROM bed_allocation WHERE id=?", Boolean.class, next))
                .isTrue();
    }

    @Test
    void FR_011a_waitsOnceThenTransfersAtActualReadyTime() {
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=? WHERE id=?",
                Timestamp.from(time.now()),
                Timestamp.from(time.now().plusSeconds(14400)),
                target);
        long next = reserve();
        clock.advanceBy(Duration.ofDays(1));
        assertThat(tx(() -> api().checkScheduledTransfer(request))).isEmpty();
        for (int i = 0; i < 2; i++)
            assertThat(tx(() -> api().applyScheduledTransfer(request, planned())))
                    .isEqualTo(ScheduledTransferResult.AWAITING_READY);
        assertThat(events.stream(ScheduledTransferWaitingForBed.class).filter(e -> e.allocationId() == next))
                .hasSize(1);
        assertThat(state(bed)).isEqualTo("OCCUPIED");
        jdbc.update("UPDATE bed SET state='EMPTY',awaiting_ready_since=NULL,ready_deadline=NULL WHERE id=?", target);
        assertThat(tx(() -> api().applyScheduledTransfer(request, planned())))
                .isEqualTo(ScheduledTransferResult.TRANSFERRED);
        assertThat(jdbc.queryForObject(
                        "SELECT waiting_for_bed_ready FROM bed_allocation WHERE id=?", Boolean.class, next))
                .isFalse();
    }

    @Test
    void FR_029_rechecksConditionsWithoutClosingOldAllocation() {
        long next = reserve();
        clock.advanceBy(Duration.ofDays(1));
        jdbc.update("UPDATE room SET isolated=TRUE WHERE id=?", room);
        assertThat(tx(() -> api().checkScheduledTransfer(request))).isNotEmpty();
        assertThatThrownBy(() -> tx(() -> api().applyScheduledTransfer(request, planned())))
                .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, old))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, next))
                .isEqualTo("FUTURE");
    }

    @Test
    void FR_029_cancellationIsIdempotentAndKeepsOldAllocation() {
        long next = reserve();
        for (int i = 0; i < 2; i++)
            tx(() -> {
                api().cancelScheduledAllocation(request, new Reason("Yêu cầu áp dụng không thành"));
                return null;
            });
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, next))
                .isEqualTo("CANCELLED");
        assertThat(state(bed)).isEqualTo("OCCUPIED");
        assertThat(state(target)).isEqualTo("EMPTY");
    }

    @Test
    void FR_029_outerFailureRollsBackReservation() {
        assertThatThrownBy(() -> tx(() -> {
                    api().reserveScheduledTransfer(request, resident, target, date);
                    throw new IllegalStateException("Lệnh duyệt004 lỗi");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE basis_type='REQUEST' AND basis_id=?",
                        Integer.class,
                        request))
                .isZero();
    }

    @Test
    void FR_029_requiresSourceTransaction() {
        assertThatThrownBy(() -> api().reserveScheduledTransfer(request, resident, target, date))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    void FR_030_urgentRequestClearsReminderAndRejectionStartsSamePriceReminderOnce() {
        jdbc.update("UPDATE bed_allocation SET awaiting_request=TRUE WHERE id=?", old);
        jdbc.update(
                "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at,reminder_count) VALUES('BED_ALLOCATION',?,'AWAITING_REQUEST',?,0)",
                old,
                Timestamp.from(time.now()));
        for (int i = 0; i < 2; i++)
            tx(() -> {
                api().requestCreatedAfterUrgentTransfer(old, request);
                return null;
            });
        assertThat(jdbc.queryForObject("SELECT awaiting_request FROM bed_allocation WHERE id=?", Boolean.class, old))
                .isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_id=? AND flag_type='AWAITING_REQUEST'",
                        Integer.class,
                        old))
                .isZero();
        for (int i = 0; i < 2; i++)
            tx(() -> {
                api().urgentTransferRequestResult(old, false);
                return null;
            });
        assertThat(jdbc.queryForObject("SELECT awaiting_same_price FROM bed_allocation WHERE id=?", Boolean.class, old))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_id=? AND flag_type='AWAITING_SAME_PRICE'",
                        Integer.class,
                        old))
                .isEqualTo(1);
    }

    @Test
    void Q_322_confirmReadinessCancelsUnsuitableWaitingAllocationAndKeepsBedEmpty() {
        long next = waitForReadiness();
        jdbc.update("UPDATE room SET gender_policy='MALE' WHERE id=?", room);
        confirmReadiness();
        assertThat(state(target)).isEqualTo("EMPTY");
        assertThat(state(bed)).isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, next))
                .isEqualTo("CANCELLED");
        assertThat(tx(() -> api().applyScheduledTransfer(request, planned())).name())
                .isEqualTo("CANCELLED");
    }

    @Test
    void FR_011a_confirmReadinessTransfersInSameTransactionWithoutLateFlag() {
        long next = waitForReadiness();
        confirmReadiness();
        assertThat(state(target)).isEqualTo("OCCUPIED");
        assertThat(state(bed)).isEqualTo("AWAITING_READY");
        assertThat(jdbc.queryForObject("SELECT executed_late FROM bed_allocation WHERE id=?", Boolean.class, next))
                .isFalse();
    }

    private long waitForReadiness() {
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=? WHERE id=?",
                Timestamp.from(time.now()),
                Timestamp.from(time.now().plusSeconds(14400)),
                target);
        long next = reserve();
        clock.advanceBy(Duration.ofDays(1));
        assertThat(tx(() -> api().applyScheduledTransfer(request, planned())))
                .isEqualTo(ScheduledTransferResult.AWAITING_READY);
        return next;
    }

    private void confirmReadiness() {
        var actor = new TestUsers(jdbc, time).staff(com.eldercare.backend.common.Role.FLOOR_MANAGER);
        user.set(actor.user());
        assignments.assign(
                actor.staffId(),
                new com.eldercare.backend.administration.account.api.spi.AssignmentView(
                        com.eldercare.backend.administration.account.api.spi.AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(60),
                        time.now().plusSeconds(3600)));
        duties.floorManager(floor, actor.staffId());
        readiness.confirm(readiness.target(target), null, null, "Đã dọn giường");
    }

    @Test
    void FR_032_scheduledTransferPreservesHospitalHoldAndRemainingDeadline() {
        var until = planned().plusSeconds(172800);
        jdbc.update("UPDATE resident SET status='HOSPITALIZED' WHERE id=?", resident);
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=?,hold_until=? WHERE id=?",
                resident,
                Timestamp.from(until),
                bed);
        reserve();
        clock.advanceBy(Duration.ofDays(1));
        assertThat(tx(() -> api().applyScheduledTransfer(request, planned())))
                .isEqualTo(ScheduledTransferResult.TRANSFERRED);
        assertThat(state(target)).isEqualTo("HELD");
        assertThat(jdbc.queryForObject("SELECT hold_until FROM bed WHERE id=?", Timestamp.class, target))
                .isEqualTo(Timestamp.from(until));
    }

    @Test
    void FR_029_outerApplyFailureRestoresBothAllocationsAndBedStates() {
        long next = reserve();
        clock.advanceBy(Duration.ofDays(1));
        assertThatThrownBy(() -> tx(() -> {
                    api().applyScheduledTransfer(request, planned());
                    throw new IllegalStateException("Áp dụng004 lỗi sau chuyển");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(state(bed)).isEqualTo("OCCUPIED");
        assertThat(state(target)).isEqualTo("EMPTY");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, old))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, next))
                .isEqualTo("FUTURE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_state_history WHERE bed_id IN (?,?)", Integer.class, bed, target))
                .isZero();
    }

    @Test
    void FR_029_concurrentReservationRetriesCreateOneFutureAllocation() throws Exception {
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = pool.submit(this::reserve);
            var second = pool.submit(this::reserve);
            assertThat(first.get(20, java.util.concurrent.TimeUnit.SECONDS))
                    .isEqualTo(second.get(20, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE basis_type='REQUEST' AND basis_id=?",
                        Integer.class,
                        request))
                .isEqualTo(1);
    }

    @Test
    void FR_016a_usesApprovedPlannedTimeEvenWhenCallerPassesExecutionTime() {
        long next = reserve();
        clock.advanceBy(Duration.ofDays(1));
        tx(() -> api().applyScheduledTransfer(request, time.now()));
        assertThat(jdbc.queryForObject("SELECT planned_at FROM bed_allocation WHERE id=?", Timestamp.class, next))
                .isEqualTo(Timestamp.from(planned()));
        assertThat(jdbc.queryForObject("SELECT executed_late FROM bed_allocation WHERE id=?", Boolean.class, next))
                .isTrue();
    }

    @Test
    void FR_013b_assetFlagFailsBeforeWritesUntilMarkerRemovalIntegrationExists() {
        long next = reserve();
        clock.advanceBy(Duration.ofDays(1));
        assets.flag(target, true);
        try {
            assertThatThrownBy(() -> tx(() -> api().applyScheduledTransfer(request, planned())))
                    .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class)
                    .satisfies(error -> assertThat(
                                    ((com.eldercare.backend.common.error.BusinessException) error).errorCode())
                            .isEqualTo("SOURCE_UNAVAILABLE"));
            assertThat(jdbc.queryForObject(
                            "SELECT waiting_for_bed_ready FROM bed_allocation WHERE id=?", Boolean.class, next))
                    .isFalse();
            assertThat(state(bed)).isEqualTo("OCCUPIED");
        } finally {
            assets.flag(target, false);
        }
    }

    @Test
    void FR_030_newRequestAfterRejectionKeepsSamePriceFlagUntilApproved() {
        jdbc.update("UPDATE bed_allocation SET awaiting_request=TRUE WHERE id=?", old);
        tx(() -> {
            api().requestCreatedAfterUrgentTransfer(old, request);
            api().urgentTransferRequestResult(old, false);
            return null;
        });
        var started = jdbc.queryForObject(
                "SELECT started_at FROM allocation_reminder WHERE target_id=? AND flag_type='AWAITING_SAME_PRICE'",
                Timestamp.class,
                old);
        tx(() -> {
            api().requestCreatedAfterUrgentTransfer(old, TestUsers.nextId());
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT awaiting_same_price FROM bed_allocation WHERE id=?", Boolean.class, old))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT started_at FROM allocation_reminder WHERE target_id=? AND flag_type='AWAITING_SAME_PRICE'",
                        Timestamp.class,
                        old))
                .isEqualTo(started);
        tx(() -> {
            api().urgentTransferRequestResult(old, true);
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT awaiting_same_price FROM bed_allocation WHERE id=?", Boolean.class, old))
                .isFalse();
    }

    @Test
    void FR_032_transferUsesExtendedExpectedEndFromSourceTransaction() {
        jdbc.update(
                "UPDATE bed_allocation SET expected_end=? WHERE id=?",
                Timestamp.from(planned().plusSeconds(3600)),
                old);
        long next = reserve();
        clock.advanceBy(Duration.ofDays(2));
        var end = planned().plusSeconds(864000);
        tx(() -> {
            api().extendExpectedEnd(resident, end);
            api().applyScheduledTransfer(request, planned());
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT expected_end FROM bed_allocation WHERE id=?", Timestamp.class, next))
                .isEqualTo(Timestamp.from(end));
    }

    @Test
    void FR_032_extendedTransferDoesNotOverlapAnInterveningClosedAllocation() {
        jdbc.update(
                "UPDATE bed_allocation SET expected_end=? WHERE id=?",
                Timestamp.from(planned().plusSeconds(3600)),
                old);
        long next = reserve();
        long otherResident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[NCT lịch sử giường]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                otherResident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,'CLOSED',?,?,'BASIC','CURRENT')",
                otherResident,
                target,
                Timestamp.from(planned().plusSeconds(7200)),
                Timestamp.from(planned().plusSeconds(10800)));
        clock.advanceBy(Duration.ofDays(2));
        var end = planned().plusSeconds(864000);
        assertThat(tx(() -> {
                    api().extendExpectedEnd(resident, end);
                    return api().applyScheduledTransfer(request, planned());
                }))
                .isEqualTo(ScheduledTransferResult.TRANSFERRED);
        assertThat(jdbc.queryForObject("SELECT expected_end FROM bed_allocation WHERE id=?", Timestamp.class, next))
                .isEqualTo(Timestamp.from(end));
    }
}
