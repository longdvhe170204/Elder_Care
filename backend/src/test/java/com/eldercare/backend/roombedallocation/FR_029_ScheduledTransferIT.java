package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import com.eldercare.backend.support.fake.roombedallocation.FakeBedAssets;
import java.sql.Timestamp;
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
