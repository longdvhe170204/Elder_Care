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

    @Autowired
    com.eldercare.backend.incident.internal.alert.service.EmergencyAssistService assist;
}
