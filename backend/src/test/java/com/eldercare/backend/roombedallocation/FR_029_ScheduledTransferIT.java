package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.ScheduledTransferResult;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.roombedallocation.api.events.ScheduledTransferWaitingForBed;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import com.eldercare.backend.support.fake.roombedallocation.FakeBedAssets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 003 T030 (US3; FR-029, FR-011a, FR-030, FR-031, FR-015; research G-07; chủ dự án chốt 2026-11-06: FR-018 trượt lúc
 * xác nhận giường sẵn sàng thì 003 tự hủy, không đổi {@code ScheduledTransferResult}; gia hạn khi không có phân bổ thì bỏ
 * qua; 003 báo kết quả yêu cầu sau chuyển gấp): chuyển giường theo lịch qua {@link BedAllocationCommandApi}.
 */
@org.springframework.test.context.event.RecordApplicationEvents
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser
class FR_029_ScheduledTransferIT extends StructureTestSupport {
    @Autowired
    BedAllocationCommandApi commands;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ApplicationEvents events;

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeAllocationResidencySource residency;

    @Autowired
    FakeAllocationInfectionStatus infection;

    @Autowired
    FakeBedAssets assets;

    @Autowired
    FakeDuties duties;

    @Autowired
    FakeAssignments assignments;

    long resident, current, next, request;
    LocalDate effective;

    @BeforeEach
    void scheduledFixture() {
        infection.reset();
        assets.reset();
        resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,"
                        + "created_at,created_by) VALUES(?,'[Họ tên NCT]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        current = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,?,'ACTIVE','2026-01-01','BASIC','CURRENT')",
                current,
                resident,
                bed);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        next = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", next, room);
        request = TestUsers.nextId();
        effective = time.today().plusDays(5);
    }

    long reserve() {
        return tx.execute(s -> commands.reserveScheduledTransfer(request, resident, next, effective));
    }

    java.util.List<com.eldercare.backend.common.error.UnmetCondition> conditions(
            java.util.function.Supplier<java.util.List<com.eldercare.backend.common.error.UnmetCondition>> check) {
        return tx.execute(s -> check.get());
    }

    String bedState(long id) {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id);
    }

    String allocation(long id) {
        return jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id);
    }

    void awaitingReady(long bedId) {
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=? WHERE id=?",
                Timestamp.from(time.now().minusSeconds(3600)),
                Timestamp.from(time.now().plusSeconds(10800)),
                bedId);
    }

    @Test
    void FR_029_approvalReservesAFutureAllocationLinkedToTheRequest() {
        assertThat(conditions(() -> commands.checkScheduledReservation(resident, next, effective)))
                .isEmpty();
        assertThat(conditions(() -> commands.checkScheduledReservation(resident, bed, effective)))
                .as("giường hiện tại")
                .isNotEmpty();
        long id = reserve();
        assertThat(jdbc.queryForMap(
                        "SELECT status,bed_id,basis_type,basis_id,previous_allocation_id,start_at FROM bed_allocation"
                                + " WHERE id=?",
                        id))
                .containsEntry("status", "FUTURE")
                .containsEntry("bed_id", next)
                .containsEntry("basis_type", "APPROVAL_REQUEST")
                .containsEntry("basis_id", request)
                .containsEntry("previous_allocation_id", current);
        assertThat(jdbc.queryForObject("SELECT start_at FROM bed_allocation WHERE id=?", Timestamp.class, id)
                        .toInstant())
                .isEqualTo(effective.atStartOfDay(BusinessTime.ZONE).toInstant());
        assertThat(conditions(() -> commands.checkScheduledTransfer(request))).isEmpty();
    }

    @Test
    void FR_029_effectiveDateWithAnEmptyBedTransfersAndClosesTheOldAllocation() {
        long id = reserve();
        var result = tx.execute(s -> commands.applyScheduledTransfer(
                request, effective.atStartOfDay(BusinessTime.ZONE).toInstant()));
        assertThat(result).isEqualTo(ScheduledTransferResult.TRANSFERRED);
        assertThat(allocation(current)).isEqualTo("CLOSED");
        assertThat(allocation(id)).isEqualTo("ACTIVE");
        assertThat(bedState(next)).isEqualTo("OCCUPIED");
        assertThat(bedState(bed)).isEqualTo("AWAITING_READY");
        assertThat(events.stream(BedTransferred.class).filter(e -> e.residentId() == resident))
                .hasSize(1);
    }

    @Test
    void FR_011a_bedNotReadyWaitsThenTransfersWhenReadinessIsConfirmed() throws Exception {
        long id = reserve();
        awaitingReady(next);
        var result = tx.execute(s -> commands.applyScheduledTransfer(
                request, effective.atStartOfDay(BusinessTime.ZONE).toInstant()));
        assertThat(result).isEqualTo(ScheduledTransferResult.AWAITING_READY);
        assertThat(allocation(id)).isEqualTo("FUTURE");
        assertThat(jdbc.queryForObject(
                        "SELECT waiting_for_bed_ready FROM bed_allocation WHERE id=?", Boolean.class, id))
                .isTrue();
        assertThat(events.stream(ScheduledTransferWaitingForBed.class).filter(e -> e.allocationId() == id))
                .hasSize(1);
        assertThat(conditions(() -> commands.checkScheduledTransfer(request)))
                .as("giường chưa Trống không phải vi phạm")
                .isEmpty();

        floorManagerOf(next);
        confirmReadiness(next).andExpect(status().isOk());
        assertThat(allocation(id)).isEqualTo("ACTIVE");
        assertThat(allocation(current)).isEqualTo("CLOSED");
        assertThat(bedState(next)).isEqualTo("OCCUPIED");
    }

    @Test
    void FR_029_conditionsFailingWhenTheBedBecomesReadyCancelTheFutureAndNotify() throws Exception {
        long id = reserve();
        awaitingReady(next);
        tx.executeWithoutResult(s -> commands.applyScheduledTransfer(
                request, effective.atStartOfDay(BusinessTime.ZONE).toInstant()));
        jdbc.update("UPDATE room SET gender_policy='MALE' WHERE id=?", room);
        assertThat(conditions(() -> commands.checkScheduledTransfer(request))).isNotEmpty();

        floorManagerOf(next);
        confirmReadiness(next).andExpect(status().isOk());
        assertThat(allocation(id)).isEqualTo("CANCELLED");
        assertThat(allocation(current)).as("ở lại giường cũ").isEqualTo("ACTIVE");
        assertThat(bedState(next)).isEqualTo("EMPTY");
        var recipients = events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n -> n.eventType().equals("SCHEDULED_TRANSFER_FAILED"))
                .flatMap(n -> n.recipientGroups().stream())
                .toList();
        assertThat(recipients)
                .contains(
                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))
                .anyMatch(g -> g instanceof RecipientGroup.FloorManager f && f.floorAreaId() == floor);
    }

    @Test
    void FR_029_failedApplicationCancelsTheFutureAllocationIdempotently() {
        long id = reserve();
        tx.executeWithoutResult(s -> commands.cancelScheduledAllocation(request, Reason.of("Áp dụng không thành")));
        tx.executeWithoutResult(s -> commands.cancelScheduledAllocation(request, Reason.of("Áp dụng không thành")));
        assertThat(allocation(id)).isEqualTo("CANCELLED");
        assertThat(allocation(current)).isEqualTo("ACTIVE");
        assertThat(bedState(next)).isEqualTo("EMPTY");
    }

    @Test
    void FR_029_reservationFailingFr018IsRejectedWithTheConditions() {
        jdbc.update("UPDATE room SET gender_policy='MALE' WHERE id=?", room);
        assertThatThrownBy(this::reserve).isInstanceOf(ConditionNotMetException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=? AND status='FUTURE'",
                        Integer.class,
                        resident))
                .isZero();
    }

    @Test
    void FR_030_requestAfterUrgentTransferAndItsResultUpdateTheFlagsAndNotify() {
        var executor = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        jdbc.update(
                "UPDATE bed_allocation SET awaiting_request=TRUE,opened_by_id=? WHERE id=?",
                executor.user().accountId(),
                current);
        jdbc.update(
                "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at,last_reminded_at,"
                        + "reminder_count) VALUES('BED_ALLOCATION',?,'AWAITING_REQUEST',?,NULL,0)",
                current,
                Timestamp.from(time.now()));
        tx.executeWithoutResult(s -> commands.requestCreatedAfterUrgentTransfer(current, request));
        assertThat(jdbc.queryForObject(
                        "SELECT awaiting_request FROM bed_allocation WHERE id=?", Boolean.class, current))
                .isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_id=? AND flag_type='AWAITING_REQUEST'",
                        Integer.class,
                        current))
                .isZero();
        tx.executeWithoutResult(s -> commands.urgentTransferRequestResult(current, false));
        assertThat(jdbc.queryForObject(
                        "SELECT awaiting_same_price FROM bed_allocation WHERE id=?", Boolean.class, current))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM allocation_reminder WHERE target_id=? AND flag_type='AWAITING_SAME_PRICE'",
                        Integer.class,
                        current))
                .isEqualTo(1);
        var recipients = events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n -> n.eventType().equals("URGENT_TRANSFER_REQUEST_RESULT"))
                .flatMap(n -> n.recipientGroups().stream())
                .toList();
        assertThat(recipients)
                .contains(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF))
                .anyMatch(g -> g instanceof RecipientGroup.Staff st && st.staffId() == executor.staffId());
    }

    @Test
    void FR_015_extendingWithoutAnActiveAllocationIsANoOp() {
        long other = TestUsers.nextId();
        tx.executeWithoutResult(s -> commands.extendExpectedEnd(other, Instant.parse("2099-01-01T00:00:00Z")));
    }

    void floorManagerOf(long bedId) {
        var actor = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        user.set(actor.user());
        assignments.assign(
                actor.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(60),
                        time.now().plusSeconds(3600)));
        duties.floorManager(floor, actor.staffId());
    }

    org.springframework.test.web.servlet.ResultActions confirmReadiness(long bedId) throws Exception {
        return mvc.perform(post("/api/v1/beds/" + bedId + "/confirm-readiness")
                .header("Idempotency-Key", "cr-" + TestUsers.nextId())
                .contentType("application/json")
                .content("{}"));
    }
}
