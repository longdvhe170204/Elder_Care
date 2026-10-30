package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.LicenseValidity;
import com.eldercare.backend.administration.account.api.spi.StaffStatus;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.dto.ShiftView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeLicenses;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.staffing.FakeM09Config;
import com.eldercare.backend.support.fake.staffing.FakeShifts;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 009 US2 (T014 phần chờ {@code CaTrucApi}): nhóm (a) "nhân viên có ca đang diễn ra tại tầng/khu vực theo vai trò và
 * chứng chỉ" và "Bác sĩ trực" xác định tại thời điểm gửi qua {@code ShiftApi} của 008 (BR-M13-04, FR-007, FR-008, FR-010).
 * Người vắng ca không có trong ca đang diễn ra ({@code ShiftQueriesIT} kiểm phía 008).
 */
@Import({FakeM15Config.class, FakeM09Config.class})
class BR_M13_04_ShiftGroupsIT extends M13IntegrationTestBase {

    private static final Substitution NONE = new Substitution.NoSubstitute();
    private static final String CERTIFICATE = "BASIC_LIFE_SUPPORT";

    @Autowired
    BusinessTime time;

    @Autowired
    FakeShifts shifts;

    @Autowired
    FakeDuties duties;

    @Autowired
    FakeLicenses licenses;

    @Autowired
    FakeStaffProfiles profiles;

    TestUsers users;
    long floor;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        floor = TestUsers.nextId();
    }

    @AfterEach
    void resetShifts() {
        shifts.reset();
    }

    /** Ca đang diễn ra của tầng với các nhân viên có mặt. */
    private long shiftNow(long floorAreaId, TestUser... staff) {
        long id = TestUsers.nextId();
        shifts.shift(
                new ShiftView(
                        id,
                        floorAreaId,
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(7)),
                        "IN_PROGRESS",
                        null),
                List.of(staff).stream().map(TestUser::staffId).toList());
        return id;
    }

    private NotificationRequest request(String key, RecipientGroup... groups) {
        return new NotificationRequest(
                "M04",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M04.TEST", key),
                key,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.empty(),
                Optional.of(floor),
                List.of(groups),
                List.of(new ContentPart(InformationType.GENERAL, "Nội dung")),
                Optional.of("Rút gọn"),
                Optional.of("Tiêu đề"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }

    private long send(NotificationRequest r) {
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        return ((Number) awaitRequests(r.eventKey(), 1).getFirst().get("id")).longValue();
    }

    private List<Map<String, Object>> notifications(long requestId) {
        return jdbc.queryForList(
                "SELECT recipient_id, JSON_UNQUOTE(basis) AS basis FROM notification WHERE request_id = ?"
                        + " AND recipient_type = 'STAFF' ORDER BY id",
                requestId);
    }

    private List<Long> staffRecipients(long requestId) {
        return notifications(requestId).stream()
                .map(n -> ((Number) n.get("recipient_id")).longValue())
                .toList();
    }

    private List<String> outcomes(long requestId) {
        return jdbc.queryForList(
                "SELECT outcome FROM notification_request_recipient_group WHERE request_id = ? ORDER BY seq",
                String.class,
                requestId);
    }

    @Test
    void FR_007_onShiftStaff_presentMembersOfTheFloorShiftFilteredByRole() {
        TestUser nurse = users.staff(Role.NURSING_STAFF);
        TestUser caregiver = users.staff(Role.CAREGIVER);
        TestUser otherFloorNurse = users.staff(Role.NURSING_STAFF);
        TestUser offShiftNurse = users.staff(Role.NURSING_STAFF);
        shiftNow(floor, nurse, caregiver);
        shiftNow(TestUsers.nextId(), otherFloorNurse);

        long nurses = send(request(
                "shift-role-" + floor,
                new RecipientGroup.OnShiftStaff(floor, Set.of(Role.NURSING_STAFF), Set.of(), NONE)));
        long everyone =
                send(request("shift-all-" + floor, new RecipientGroup.OnShiftStaff(floor, Set.of(), Set.of(), NONE)));

        assertThat(staffRecipients(nurses)).containsExactly(nurse.staffId());
        assertThat((String) notifications(nurses).getFirst().get("basis")).contains("OnShiftStaff");
        assertThat(outcomes(nurses)).containsExactly("HAS_RECIPIENTS");
        assertThat(staffRecipients(everyone)).containsExactlyInAnyOrder(nurse.staffId(), caregiver.staffId());
        assertThat(staffRecipients(everyone)).doesNotContain(offShiftNurse.staffId(), otherFloorNurse.staffId());
    }

    @Test
    void FR_007_onShiftStaff_requiresEveryCertificateValidToday() {
        TestUser certified = users.staff(Role.NURSING_STAFF);
        TestUser expired = users.staff(Role.NURSING_STAFF);
        TestUser none = users.staff(Role.NURSING_STAFF);
        licenses.license(
                certified.staffId(),
                CERTIFICATE,
                new LicenseValidity(TestUsers.nextId(), "Cấp cứu cơ bản", time.today(), Set.of()));
        licenses.license(
                expired.staffId(),
                CERTIFICATE,
                new LicenseValidity(
                        TestUsers.nextId(), "Cấp cứu cơ bản", time.today().minusDays(1), Set.of()));
        shiftNow(floor, certified, expired, none);

        long id = send(request(
                "shift-cert-" + floor,
                new RecipientGroup.OnShiftStaff(floor, Set.of(Role.NURSING_STAFF), Set.of(CERTIFICATE), NONE)));

        assertThat(staffRecipients(id)).containsExactly(certified.staffId());
    }

    @Test
    void FR_008_FR_010_onShiftStaff_resignedExcluded_noOneLeftGoesToFloorManager() {
        TestUser resigned = users.staff(Role.CAREGIVER);
        TestUser floorManager = users.staff(Role.FLOOR_MANAGER);
        profiles.status(resigned.staffId(), StaffStatus.RESIGNED);
        duties.floorManager(floor, floorManager.staffId());
        shiftNow(floor, resigned);

        long id = send(request(
                "shift-sub-" + floor,
                new RecipientGroup.OnShiftStaff(floor, Set.of(), Set.of(), new Substitution.Default())));

        assertThat(staffRecipients(id)).containsExactly(floorManager.staffId());
        assertThat((String) notifications(id).getFirst().get("basis")).contains("SUBSTITUTE:OnShiftStaff");
        assertThat(outcomes(id)).containsExactly("SUBSTITUTED");
    }

    @Test
    void FR_007_onDutyDoctor_everyDoctorOnDutyReceives() {
        TestUser doctor = users.staff(Role.DOCTOR);
        TestUser second = users.staff(Role.DOCTOR);
        TestUser offDuty = users.staff(Role.DOCTOR);
        long facilityShift = shiftNow(TestUsers.nextId(), doctor, second, offDuty);
        shifts.onDutyDoctor(facilityShift, doctor.staffId()).onDutyDoctor(facilityShift, second.staffId());

        long id = send(request("doctor-" + floor, new RecipientGroup.OnDutyDoctor(NONE)));

        assertThat(staffRecipients(id)).containsExactlyInAnyOrder(doctor.staffId(), second.staffId());
        assertThat((String) notifications(id).getFirst().get("basis")).contains("OnDutyDoctor");
    }

    @Test
    void FR_010_onDutyDoctor_noneOnDuty_followsDefaultSubstitution() {
        TestUser floorManager = users.staff(Role.FLOOR_MANAGER);
        duties.floorManager(floor, floorManager.staffId());

        long id = send(request("doctor-sub-" + floor, new RecipientGroup.OnDutyDoctor(new Substitution.Default())));

        assertThat(staffRecipients(id)).containsExactly(floorManager.staffId());
        assertThat(outcomes(id)).containsExactly("SUBSTITUTED");
    }
}
