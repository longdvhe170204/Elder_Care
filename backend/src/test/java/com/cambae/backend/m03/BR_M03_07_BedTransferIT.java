package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m03.api.events.BedTransferred;
import com.cambae.backend.m03.api.events.DifferentPriceBedTransfer;
import com.cambae.backend.m03.api.spi.ResidencyType;
import com.cambae.backend.m03.internal.allocation.service.BedTransferService;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeAllocationInfectionStatus;
import com.cambae.backend.support.fake.m03.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.AopTestUtils;

/** T029/T032: transaction thật, không có transaction bao test để che lỗi hoàn tác. */
@RecordApplicationEvents
class BR_M03_07_BedTransferIT extends StructureTestSupport {
    @Autowired
    BedTransferService transfers;

    @Autowired
    ApplicationEvents published;

    @MockitoSpyBean
    FakeAllocationResidencySource residency;

    @Autowired
    FakeAllocationInfectionStatus infection;

    long resident, allocation, target, targetRoom, targetFloor, transferReason;

    @BeforeEach
    void transferFixture() {
        infection.reset();
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        allocation = TestUsers.nextId();
        target = TestUsers.nextId();
        targetRoom = TestUsers.nextId();
        targetFloor = TestUsers.nextId();
        transferReason = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now().minusSeconds(60)),
                Timestamp.from(time.now().minusSeconds(60)),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)",
                targetFloor,
                area,
                "F" + targetFloor,
                "Tầng đích");
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                targetRoom,
                targetFloor,
                type,
                "R" + targetRoom,
                "Phòng đích");
        jdbc.update("INSERT INTO room_allowed_care_level VALUES(?,'BASIC')", targetRoom);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'T1')", target, targetRoom);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,'ACTIVE',?,'BASIC','CURRENT')",
                allocation,
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(60)));
        jdbc.update(
                "INSERT INTO bed_transfer_reason(id,code,name,medical_safety) VALUES(?,?,?,FALSE)",
                transferReason,
                "TR" + transferReason,
                "Theo nhu cầu " + transferReason);
    }

    private long transfer(long targetId, long reasonId, Reason reason) {
        return transfers.transfer(allocation, targetId, reasonId, reason);
    }

    @Test
    void BR_M03_07_closesAndOpensAtSameInstantWithMetadataBedStatesAuditAndEvent() throws Exception {
        long next = transfer(target, transferReason, new Reason("Chuyển tầng"));
        var previous = jdbc.queryForMap("SELECT status,end_at,end_reason FROM bed_allocation WHERE id=?", allocation);
        var current = jdbc.queryForMap(
                "SELECT status,start_at,previous_allocation_id,transfer_reason_id FROM bed_allocation WHERE id=?",
                next);
        assertThat(previous).containsEntry("status", "CLOSED").containsEntry("end_reason", "BED_TRANSFER");
        assertThat(current)
                .containsEntry("status", "ACTIVE")
                .containsEntry("previous_allocation_id", allocation)
                .containsEntry("transfer_reason_id", transferReason);
        assertThat(previous.get("end_at")).isEqualTo(current.get("start_at"));
        assertThat(state(bed)).isEqualTo("AWAITING_READY");
        assertThat(state(target)).isEqualTo("OCCUPIED");
        var events = published.stream(BedTransferred.class)
                .filter(e -> e.residentId() == resident)
                .toList();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().previousAllocationId()).isEqualTo(allocation);
        assertThat(events.getFirst().nextAllocationId()).isEqualTo(next);
        assertThat(events.getFirst().previousFloorId()).isEqualTo(floor);
        assertThat(events.getFirst().nextFloorId()).isEqualTo(targetFloor);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='M03_BED_TRANSFER' AND resident_id=? AND reason='Chuyển tầng'",
                        Integer.class,
                        resident))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_id=? AND flag_type='AWAITING_REQUEST'",
                        Integer.class,
                        next))
                .isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEMPORARILY_ABSENT", "HOSPITALIZED"})
    void FR_032_absentResidentPreservesHoldAndDeadline(String status) throws Exception {
        var until = time.now().plusSeconds(3600).truncatedTo(ChronoUnit.MICROS);
        jdbc.update("UPDATE resident SET status=? WHERE id=?", status, resident);
        jdbc.update(
                "UPDATE bed SET state='HELD',hold_reason='ABSENT_RESIDENT',hold_resident_id=?,hold_until=? WHERE id=?",
                resident,
                Timestamp.from(until),
                bed);
        transfer(target, transferReason, new Reason("Chuyển khi đang vắng"));
        assertThat(state(target)).isEqualTo("HELD");
        assertThat(jdbc.queryForMap("SELECT hold_reason,hold_resident_id FROM bed WHERE id=?", target))
                .containsEntry("hold_reason", "ABSENT_RESIDENT")
                .containsEntry("hold_resident_id", resident);
        assertThat(jdbc.queryForObject("SELECT hold_until FROM bed WHERE id=?", Timestamp.class, target)
                        .toInstant())
                .isEqualTo(until);
        assertThat(state(bed)).isEqualTo("AWAITING_READY");
    }

    @Test
    void FR_032_offSiteActivityDoesNotBecomeAnAbsentHold() throws Exception {
        jdbc.update("UPDATE resident SET status='OFF_SITE_ACTIVITY' WHERE id=?", resident);
        transfer(target, transferReason, new Reason("Đổi giường khi đi hoạt động"));
        assertThat(state(target)).isEqualTo("OCCUPIED");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void FR_028_lateSourceFailureRollsBackAllWrites(boolean priceChange) {
        if (priceChange) {
            differentPrice();
            jdbc.update("UPDATE bed_transfer_reason SET medical_safety=TRUE WHERE id=?", transferReason);
        }
        doThrow(new IllegalStateException("Nguồn004 lỗi sau đổi giường"))
                .when(AopTestUtils.<FakeAllocationResidencySource>getUltimateTargetObject(residency))
                .allocated(eq(resident), anyLong());
        assertThatThrownBy(() -> transfer(target, transferReason, new Reason("Thử hoàn tác")))
                .isInstanceOf(IllegalStateException.class);
        unchanged();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_state_history WHERE bed_id IN (?,?)", Integer.class, bed, target))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='M03_BED_TRANSFER' AND resident_id=?",
                        Integer.class,
                        resident))
                .isZero();
        assertThat(published.stream(BedTransferred.class).filter(e -> e.residentId() == resident))
                .isEmpty();
    }

    @Test
    void FR_033_twoTransfersOnSameAllocationHaveOneWinner() throws Exception {
        long other = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'T2')", other, targetRoom);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (long next : new long[] {target, other})
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Chưa mở cổng đồng thời");
                    try {
                        transfer(next, transferReason, new Reason("Chuyển đồng thời"));
                        return true;
                    } catch (BusinessException error) {
                        assertThat(error.errorCode()).isEqualTo("ALLOCATION_NOT_TRANSFERABLE");
                        return false;
                    }
                }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int won = 0;
            for (var result : results) if (result.get(30, TimeUnit.SECONDS)) won++;
            assertThat(won).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=? AND status='ACTIVE'",
                        Integer.class,
                        resident))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=?", Integer.class, resident))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed WHERE id IN (?,?) AND state='OCCUPIED'",
                        Integer.class,
                        target,
                        other))
                .isEqualTo(1);
    }

    @Test
    void FR_030_nonMedicalDifferentPriceCannotTransfer() {
        differentPrice();
        assertThatThrownBy(() -> transfer(target, transferReason, new Reason("Đổi phòng khác giá")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("RESIDENCY_CHANGE_REQUEST_REQUIRED"));
        unchanged();
    }

    @Test
    void FR_030_medicalDifferentPriceSetsFlagReminderAndNotificationAtomically() throws Exception {
        differentPrice();
        jdbc.update("UPDATE bed_transfer_reason SET medical_safety=TRUE WHERE id=?", transferReason);
        long next = transfer(target, transferReason, new Reason("Cần an toàn"));
        assertThat(jdbc.queryForObject("SELECT awaiting_request FROM bed_allocation WHERE id=?", Boolean.class, next))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_type='BED_ALLOCATION' AND target_id=? AND flag_type='AWAITING_REQUEST' AND reminder_count=0",
                        Integer.class,
                        next))
                .isEqualTo(1);
        var events = published.stream(DifferentPriceBedTransfer.class)
                .filter(e -> e.residentId() == resident)
                .toList();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().notification().recipients())
                .containsExactlyInAnyOrder(
                        new com.cambae.backend.common.notification.RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                        new com.cambae.backend.common.notification.RecipientGroup.FacilityRole(
                                Role.NURSING_HOME_MANAGER));
    }

    @Test
    void FR_010_isolatedSourceRequiresControlledTransfer() {
        jdbc.update("UPDATE room SET isolated=TRUE WHERE id=?", room);
        assertThatThrownBy(() -> transfer(target, transferReason, new Reason("Không dùng lệnh ngoại lệ")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("INFECTION_CONTROL_TRANSFER_REQUIRED"));
        unchanged();
    }

    @Test
    void FR_018_targetPoliciesAreCheckedAgainBeforeWrite() {
        jdbc.update("DELETE FROM room_allowed_care_level WHERE room_id=?", targetRoom);
        assertThatThrownBy(() -> transfer(target, transferReason, new Reason("Phòng không phù hợp")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("UNMET_CONDITIONS"));
        unchanged();
    }

    @Test
    void FR_028_inactiveCatalogReasonIsRejected() {
        jdbc.update("UPDATE bed_transfer_reason SET status='INACTIVE' WHERE id=?", transferReason);
        assertThatThrownBy(() -> transfer(target, transferReason, new Reason("Không dùng lý do ngừng")))
                .isInstanceOf(BusinessException.class);
        unchanged();
    }

    @Test
    void FR_028_reasonAndRealPermissionAreRequired() {
        assertThatThrownBy(() -> transfer(target, transferReason, null)).isInstanceOf(BusinessException.class);
        user.set(new TestUsers(jdbc, time).staff(Role.CAREGIVER).user());
        assertThatThrownBy(() -> transfer(target, transferReason, new Reason("Không có quyền")))
                .isInstanceOf(BusinessException.class);
        unchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWAITING_REQUEST", "AWAITING_SAME_PRICE"})
    void FR_030_pendingPriceResolutionDoesNotLoseFlagOrReminder(String flag) {
        String column = flag.equals("AWAITING_REQUEST") ? "awaiting_request" : "awaiting_same_price";
        jdbc.update("UPDATE bed_allocation SET " + column + "=TRUE WHERE id=?", allocation);
        var started = time.now().minusSeconds(3600).truncatedTo(ChronoUnit.MICROS);
        jdbc.update(
                "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at,last_reminded_at,reminder_count) VALUES('BED_ALLOCATION',?,?,?,NULL,0)",
                allocation,
                flag,
                Timestamp.from(started));
        assertThatThrownBy(() -> transfer(target, transferReason, new Reason("Chuyển tiếp khi còn nghĩa vụ")))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.errorCode()).isEqualTo("SOURCE_UNAVAILABLE");
                    assertThat(error.status().value()).isEqualTo(503);
                });
        unchanged();
        assertThat(jdbc.queryForObject(
                        "SELECT " + column + " FROM bed_allocation WHERE id=?", Boolean.class, allocation))
                .isTrue();
        assertThat(jdbc.queryForObject(
                                "SELECT started_at FROM allocation_reminder WHERE target_type='BED_ALLOCATION' AND target_id=? AND flag_type=?",
                                Timestamp.class,
                                allocation,
                                flag)
                        .toInstant())
                .isEqualTo(started);
        assertThat(published.stream(BedTransferred.class).filter(e -> e.residentId() == resident))
                .isEmpty();
    }

    private void differentPrice() {
        residency.priceDifference(resident, type, type, time.today(), true);
    }

    private String state(long id) {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id);
    }

    private void unchanged() {
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, allocation))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=?", Integer.class, resident))
                .isEqualTo(1);
        assertThat(state(bed)).isEqualTo("OCCUPIED");
        assertThat(state(target)).isEqualTo("EMPTY");
    }
}
