package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AlertResponsibility;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 007 T063, 008 T043b (chạy lại US3 với bean thật thay T002, 2026-11-11): không có {@code @MockitoBean} của
 * {@link AlertStaffPort}; tầng của người cao tuổi đọc giường thật (003), Điều dưỡng phụ trách đọc phân công thật trong ca
 * (008 {@code AlertStaffLookup}), Người phụ trách ca và Trưởng tầng đọc 008, 002 thật, đổi mã nhân viên sang tài khoản qua
 * 002. Cảnh báo chăm sóc tạo ra giao cho Điều dưỡng phụ trách; khi không có ai phụ trách thì Người phụ trách ca. US8: truy
 * vết nhân viên tiếp xúc đọc phân công thật.
 */
class T063_RealResponsibilityIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    AlertResponsibility responsibility;

    @Autowired
    AlertStaffPort staffPort;

    @Autowired
    AlertService alerts;

    @Autowired
    TransactionTemplate tx;

    @AfterEach
    void restore() {
        clock.reset();
    }

    @Test
    void US3_realLocationShiftAndAssignment_resolveFloorAssigneeAndProvisionalAssignee() {
        clock.setTo(time.today().atTime(9, 0).atZone(BusinessTime.ZONE).toInstant());
        Instant now = time.now();
        var fixture = new RealCareFixture(jdbc, time);
        var place = fixture.place();
        long resident = fixture.residentInBed(place, now.minus(Duration.ofDays(5)));
        long unassigned = fixture.residentInBed(fixture.place(), now.minus(Duration.ofDays(5)));
        TestUser nurse = fixture.staff(Role.NURSING_STAFF);
        TestUser lead = fixture.staff(Role.NURSING_STAFF);
        TestUser manager = fixture.staff(Role.FLOOR_MANAGER);
        fixture.floorManager(manager.staffId(), place.floor(), now.minus(Duration.ofDays(30)));
        long shift = fixture.shift(place.floor(), now.minus(Duration.ofHours(2)), 8);
        fixture.member(shift, nurse.staffId());
        fixture.member(shift, lead.staffId());
        fixture.lead(shift, lead.staffId(), now.minus(Duration.ofHours(2)));
        fixture.assignResponsibleNurse(
                shift, nurse.staffId(), resident, now.minus(Duration.ofHours(2)), now.plus(Duration.ofHours(6)));

        assertThat(responsibility.floorOf(resident, now)).contains(place.floor());
        assertThat(staffPort.assignedNurse(resident, now)).contains(nurse.staffId());
        assertThat(responsibility.initialAssignee(resident, place.floor(), now)).contains(nurse.accountId());
        assertThat(responsibility.provisionalAssignee(place.floor(), now)).contains(lead.accountId());
        assertThat(staffPort.assignedFloorManager(place.floor(), now)).contains(manager.staffId());

        long alert = tx.execute(s -> alerts.requestAlert(new CreateAlertCommand(
                        resident,
                        place.floor(),
                        "LOW_WATER_INTAKE",
                        "LOW_WATER_INTAKE",
                        IncidentLevel.MEDIUM,
                        now,
                        "CARE_TASK_RESULT",
                        TestUsers.nextId(),
                        "Uống nước chưa đủ",
                        null,
                        null))
                .orElseThrow()
                .id());
        assertThat(jdbc.queryForObject("SELECT assignee_id FROM alert WHERE id=?", Long.class, alert))
                .isEqualTo(nurse.accountId());
        // US8 (FR-060 (a)): truy vết nhân viên tiếp xúc đọc phân công thật trong khoảng.
        assertThat(staffPort.assignmentsInRange(resident, now.minus(Duration.ofHours(1)), now))
                .extracting(v -> v.staffId())
                .contains(nurse.staffId());
        // Người không có Điều dưỡng phụ trách ở tầng khác không có ca: không ai nhận ban đầu.
        assertThat(staffPort.assignedNurse(unassigned, now)).isEmpty();
    }

    /**
     * 008 T049b, 007 US13 (quickstart H1, H3; FR-047i, BR-M05-29) với {@link AlertStaffPort} thật của 008 (không bản giả
     * Mockito như {@code AssistTestBase}): thành viên ca đang diễn ra bấm nút hỗ trợ thì cảnh báo Khẩn cấp chưa gắn người
     * cao tuổi mang tầng của ca ({@code onShift}, {@code floorDuringShift}); tầng có ca nằm trong {@code floorsWithShift};
     * nhân viên không có tên trong ca nào bị từ chối 409 {@code NOT_ON_SHIFT}.
     */
    @Test
    void T049b_H1_H3_assistButtonWithRealShiftStaff() {
        clock.setTo(time.today().atTime(9, 0).atZone(BusinessTime.ZONE).toInstant());
        Instant now = time.now();
        var fixture = new RealCareFixture(jdbc, time);
        var place = fixture.place();
        TestUser presser = fixture.staff(Role.NURSING_STAFF);
        TestUser offShift = fixture.staff(Role.NURSING_STAFF);
        long shift = fixture.shift(place.floor(), now.minus(Duration.ofHours(1)), 8);
        fixture.member(shift, presser.staffId());

        assertThat(staffPort.onShift(presser.staffId(), now)).isTrue();
        assertThat(staffPort.floorDuringShift(presser.staffId(), now)).contains(place.floor());
        assertThat(staffPort.floorsWithShift(now)).contains(place.floor());
        assertThat(staffPort.onShift(offShift.staffId(), now)).isFalse();

        tx.executeWithoutResult(s -> assist.press(presser.user(), "assist-" + TestUsers.nextId()));
        var alert = jdbc.queryForMap(
                "SELECT resident_id, floor_area_id, level FROM alert WHERE type='STAFF_ASSIST_REQUEST' AND requester_id=?",
                presser.accountId());
        assertThat(alert.get("resident_id")).isNull();
        assertThat(((Number) alert.get("floor_area_id")).longValue()).isEqualTo(place.floor());
        assertThat(alert.get("level")).isEqualTo("CRITICAL");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        tx.executeWithoutResult(s -> assist.press(offShift.user(), "assist-" + TestUsers.nextId())))
                .isInstanceOfSatisfying(
                        com.eldercare.backend.common.error.BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("NOT_ON_SHIFT"));
    }

    @Autowired
    com.eldercare.backend.incident.internal.alert.service.EmergencyAssistService assist;
}
