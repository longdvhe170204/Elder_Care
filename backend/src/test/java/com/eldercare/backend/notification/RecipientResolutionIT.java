package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.StaffStatus;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.notification.FakeFamilyNotifications;
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
 * 009 US2 (T014 phần không cần {@code CaTrucApi}): BR-M13-04 người nhận theo phân
 * công tại thời điểm gửi; FR-008 loại trừ; FR-010 cách thay; FR-011 không có người nhận; FR-012 gộp người nhận, người gây
 * ra; FR-013 lọc phần nội dung người thân; FR-014, FR-014a phạm vi "nhận thông báo" và mã trường hợp. Người cao tuổi
 * ở đây không có bản đồng ý Hiệu lực, nên người thân ngoài phạm vi và không thấy phần sức khỏe; trường hợp có bản đồng ý ở
 * {@link FR_014_015_ConsentAndDeathIT}.
 */
@Import(FakeM15Config.class)
class RecipientResolutionIT extends M13IntegrationTestBase {

    private static final Substitution DEFAULT = new Substitution.Default();

    @Autowired
    BusinessTime time;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeDuties duties;

    @Autowired
    FakeStaffProfiles profiles;

    @Autowired
    FakeFamilyNotifications family;

    TestUsers users;
    long resident;
    long floor;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        resident = TestUsers.nextId();
        floor = TestUsers.nextId();
    }

    @AfterEach
    void resetFamily() {
        family.reset();
    }

    private void assignTo(TestUser staff, long residentId) {
        assignments.assign(
                staff.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentId,
                        null,
                        time.now().minus(Duration.ofHours(1)),
                        null));
    }

    private NotificationRequest request(
            String key,
            NotificationLevel level,
            List<RecipientGroup> groups,
            Optional<Long> causedBy,
            Optional<InformationSubject> subject,
            Optional<Q354Case> q354Case,
            InformationType... parts) {
        List<ContentPart> content = (parts.length == 0 ? List.of(InformationType.GENERAL) : List.of(parts))
                .stream().map(t -> new ContentPart(t, "Nội dung " + t)).toList();
        return new NotificationRequest(
                "M05",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M05.TEST", key),
                key,
                Optional.of(level),
                Optional.of(resident),
                Optional.of(floor),
                groups,
                content,
                Optional.of("Rút gọn"),
                Optional.of("Tiêu đề"),
                causedBy,
                false,
                time.now(),
                Map.of(),
                false,
                subject,
                q354Case);
    }

    private NotificationRequest staffRequest(String key, NotificationLevel level, RecipientGroup... groups) {
        return request(key, level, List.of(groups), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** Gửi một yêu cầu; trả id yêu cầu đã lưu. */
    private long send(NotificationRequest r) {
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        return ((Number) awaitRequests(r.eventKey(), 1).getFirst().get("id")).longValue();
    }

    private List<Map<String, Object>> notifications(long requestId) {
        return jdbc.queryForList(
                "SELECT recipient_type, recipient_id, account_id, JSON_UNQUOTE(basis) AS basis, visible_parts,"
                        + " identity_only, q354_case FROM notification WHERE request_id = ? ORDER BY id",
                requestId);
    }

    private Map<String, Object> notificationOf(long requestId, String type, long recipientId) {
        return notifications(requestId).stream()
                .filter(n -> type.equals(n.get("recipient_type"))
                        && ((Number) n.get("recipient_id")).longValue() == recipientId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có thông báo cho " + type + ":" + recipientId));
    }

    private List<Long> staffRecipients(long requestId) {
        return notifications(requestId).stream()
                .filter(n -> "STAFF".equals(n.get("recipient_type")))
                .map(n -> ((Number) n.get("recipient_id")).longValue())
                .toList();
    }

    private String flags(long requestId) {
        return jdbc.queryForObject("SELECT flags FROM notification_request WHERE id = ?", String.class, requestId);
    }

    private List<String> outcomes(long requestId) {
        return jdbc.queryForList(
                "SELECT outcome FROM notification_request_recipient_group WHERE request_id = ? ORDER BY seq",
                String.class,
                requestId);
    }

    @Test
    void BR_M13_04_assignedNurse_isNurseAssignedNow() {
        TestUser nurse = users.staff(Role.NURSING_STAFF);
        TestUser caregiver = users.staff(Role.CAREGIVER);
        assignTo(nurse, resident);
        assignTo(caregiver, resident);

        long id = send(staffRequest(
                "br04-" + resident, NotificationLevel.MEDIUM, new RecipientGroup.AssignedNurse(resident, DEFAULT)));

        assertThat(staffRecipients(id)).containsExactly(nurse.staffId());
        Map<String, Object> n = notificationOf(id, "STAFF", nurse.staffId());
        assertThat(((Number) n.get("account_id")).longValue()).isEqualTo(nurse.accountId());
        assertThat((String) n.get("basis")).contains("AssignedNurse");
        assertThat(outcomes(id)).containsExactly("HAS_RECIPIENTS");
        // Lần gửi dự kiến trong ứng dụng (kênh khác chọn ở US3)
        assertThat(jdbc.queryForObject(
                        "SELECT channel FROM delivery_attempt a JOIN notification n ON n.id = a.notification_id"
                                + " WHERE n.request_id = ?",
                        String.class,
                        id))
                .isEqualTo("IN_APP");
    }

    @Test
    void FR_010_defaultSubstitution_shiftLeadThenFloorManagerThenItsSubstitute() {
        TestUser shiftLead = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, shiftLead.staffId());

        long nurseMissing = send(staffRequest(
                "fr010a-" + resident, NotificationLevel.MEDIUM, new RecipientGroup.AssignedNurse(resident, DEFAULT)));

        assertThat(staffRecipients(nurseMissing)).containsExactly(shiftLead.staffId());
        assertThat((String) notificationOf(nurseMissing, "STAFF", shiftLead.staffId())
                        .get("basis"))
                .contains("SUBSTITUTE:AssignedNurse");
        assertThat(outcomes(nurseMissing)).containsExactly("SUBSTITUTED");

        TestUser acting = users.staff(Role.NURSING_STAFF);
        duties.floorManagerSubstitute(floor, acting.staffId());
        long noFloorManager = send(staffRequest(
                "fr010b-" + resident, NotificationLevel.MEDIUM, new RecipientGroup.FloorManager(floor, DEFAULT)));

        assertThat(staffRecipients(noFloorManager)).containsExactly(acting.staffId());
    }

    @Test
    void FR_010_noSubstitute_skipped_designated_used() {
        TestUser doctor = users.staff(Role.DOCTOR);
        TestUser accountant = users.staff(Role.ACCOUNTANT);
        assignTo(accountant, resident);

        long id = send(staffRequest(
                "fr010c-" + resident,
                NotificationLevel.MEDIUM,
                new RecipientGroup.AssignedNurse(resident, new Substitution.NoSubstitute()),
                new RecipientGroup.AssignedNurse(
                        resident,
                        new Substitution.Designated(new RecipientGroup.Staff(doctor.staffId(), "DESIGNATED")))));

        assertThat(staffRecipients(id)).containsExactly(doctor.staffId());
        assertThat(outcomes(id)).containsExactly("SKIPPED_NO_SUBSTITUTE", "SUBSTITUTED");
        assertThat(flags(id)).doesNotContain("NO_RECIPIENT");
    }

    @Test
    void FR_008_resignedAndLockedExcluded_temporarilyLockedKept() {
        TestUser resigned = users.staff(Role.NURSING_STAFF);
        TestUser locked = users.staff(Role.NURSING_STAFF);
        TestUser tempLocked = users.staff(Role.NURSING_STAFF);
        for (TestUser u : List.of(resigned, locked, tempLocked)) {
            assignTo(u, resident);
        }
        profiles.status(resigned.staffId(), StaffStatus.RESIGNED);
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL' WHERE id = ?", locked.accountId());
        jdbc.update(
                "UPDATE account SET status = 'TEMPORARILY_LOCKED', locked_until = UTC_TIMESTAMP(6) + INTERVAL 1 HOUR"
                        + " WHERE id = ?",
                tempLocked.accountId());

        long id = send(staffRequest(
                "fr008-" + resident,
                NotificationLevel.MEDIUM,
                new RecipientGroup.AssignedNurse(resident, new Substitution.NoSubstitute())));

        assertThat(staffRecipients(id)).containsExactly(tempLocked.staffId());
    }

    @Test
    void FR_011_noRecipient_flagged_urgentGoesToManagersAndFloorManagers() {
        TestUser manager = users.staff(Role.NURSING_HOME_MANAGER);
        TestUser ownFloorManager = users.staff(Role.FLOOR_MANAGER);
        TestUser otherFloorManager = users.staff(Role.FLOOR_MANAGER);
        duties.floorManager(floor, ownFloorManager.staffId());
        RecipientGroup none = new RecipientGroup.AssignedNurse(resident, new Substitution.NoSubstitute());

        long medium = send(staffRequest("fr011a-" + resident, NotificationLevel.MEDIUM, none));
        long urgent = send(staffRequest("fr011b-" + resident, NotificationLevel.URGENT, none));

        assertThat(notifications(medium)).isEmpty();
        assertThat(flags(medium)).contains("NO_RECIPIENT");
        assertThat(flags(urgent)).contains("NO_RECIPIENT");
        assertThat(staffRecipients(urgent))
                .contains(manager.staffId(), ownFloorManager.staffId(), otherFloorManager.staffId());
        assertThat(notificationOf(urgent, "STAFF", manager.staffId()).get("identity_only"))
                .isEqualTo(false);
        assertThat(notificationOf(urgent, "STAFF", ownFloorManager.staffId()).get("identity_only"))
                .isEqualTo(false);
        // Trưởng tầng không có phạm vi với người cao tuổi chỉ thấy thông tin nhận dạng
        assertThat(notificationOf(urgent, "STAFF", otherFloorManager.staffId()).get("identity_only"))
                .isEqualTo(true);
        assertThat((String) notificationOf(urgent, "STAFF", manager.staffId()).get("basis"))
                .contains("NO_RECIPIENT_FALLBACK");
    }

    @Test
    void FR_012_onePersonOneNotification_allBases_causerSkippedOnlyForLow() {
        TestUser nurse = users.staff(Role.NURSING_STAFF);
        assignTo(nurse, resident);
        duties.shiftLead(floor, nurse.staffId());
        List<RecipientGroup> both = List.of(
                new RecipientGroup.AssignedNurse(resident, DEFAULT), new RecipientGroup.ShiftLead(floor, DEFAULT));

        long medium = send(request(
                "fr012a-" + resident,
                NotificationLevel.MEDIUM,
                both,
                Optional.of(nurse.accountId()),
                Optional.empty(),
                Optional.empty()));
        long low = send(request(
                "fr012b-" + resident,
                NotificationLevel.LOW,
                both,
                Optional.of(nurse.accountId()),
                Optional.empty(),
                Optional.empty()));

        assertThat(staffRecipients(medium)).containsExactly(nurse.staffId());
        assertThat((String) notificationOf(medium, "STAFF", nurse.staffId()).get("basis"))
                .contains("AssignedNurse")
                .contains("ShiftLead");
        assertThat(notifications(low)).isEmpty();
    }

    @Test
    void facilityRole_andApprovers_andStaff() {
        TestUser accountant = users.staff(Role.ACCOUNTANT);
        TestUser approver = users.staff(Role.OFFICE_STAFF);
        TestUser requester = users.staff(Role.OFFICE_STAFF);

        long id = send(staffRequest(
                "groups-" + resident,
                NotificationLevel.LOW,
                new RecipientGroup.FacilityRole(Role.ACCOUNTANT),
                new RecipientGroup.Approvers("M15.SAMPLE", List.of(approver.accountId())),
                new RecipientGroup.Staff(requester.staffId(), "REQUESTER")));

        assertThat(staffRecipients(id)).contains(accountant.staffId(), approver.staffId(), requester.staffId());
        assertThat((String) notificationOf(id, "STAFF", requester.staffId()).get("basis"))
                .contains("Staff:REQUESTER");
    }

    private static NotifiableRelationshipView relation(
            long familyMemberId, boolean primary, boolean representative, Set<FamilyRight> rights) {
        return new NotifiableRelationshipView(
                familyMemberId, primary, representative, rights, Optional.empty(), false, Optional.empty());
    }

    @Test
    void FR_013_familyMatter_partsByRight_noHealthWithoutConsent() {
        long withCosts = TestUsers.nextId();
        long withHealth = TestUsers.nextId();
        family.relate(resident, relation(withCosts, false, false, Set.of(FamilyRight.VIEW_COSTS)));
        family.relate(resident, relation(withHealth, false, false, Set.of(FamilyRight.VIEW_HEALTH)));

        long id = send(request(
                "fr013-" + resident,
                NotificationLevel.LOW,
                List.of(
                        new RecipientGroup.FamilyWithRight(resident, FamilyRight.VIEW_COSTS),
                        new RecipientGroup.FamilyWithRight(resident, FamilyRight.VIEW_HEALTH)),
                Optional.empty(),
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty(),
                InformationType.GENERAL,
                InformationType.COSTS,
                InformationType.HEALTH));

        assertThat(notificationOf(id, "FAMILY_MEMBER", withCosts).get("visible_parts"))
                .isEqualTo("GENERAL,COSTS");
        // Không có bản đồng ý Hiệu lực nên phần sức khỏe không hiện (DBR-03)
        assertThat(notificationOf(id, "FAMILY_MEMBER", withHealth).get("visible_parts"))
                .isEqualTo("GENERAL");
    }

    @Test
    void FR_014_urgent_primaryContactAlways_othersNeedReceiveUrgent_caseRecorded() {
        long primary = TestUsers.nextId();
        long withUrgent = TestUsers.nextId();
        long without = TestUsers.nextId();
        family.relate(resident, relation(primary, true, false, Set.of()));
        family.relate(resident, relation(withUrgent, false, false, Set.of(FamilyRight.RECEIVE_URGENT)));
        family.relate(resident, relation(without, false, false, Set.of(FamilyRight.VIEW_COSTS)));

        long id = send(request(
                "fr014a-" + resident,
                NotificationLevel.URGENT,
                List.of(
                        new RecipientGroup.PrimaryContact(resident),
                        new RecipientGroup.FamilyWithRight(resident, FamilyRight.RECEIVE_URGENT),
                        new RecipientGroup.FamilyWithRight(resident, FamilyRight.VIEW_COSTS)),
                Optional.empty(),
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.of(Q354Case.PRIMARY_CONTACT_EMERGENCY)));

        List<Long> familyRecipients = notifications(id).stream()
                .filter(n -> "FAMILY_MEMBER".equals(n.get("recipient_type")))
                .map(n -> ((Number) n.get("recipient_id")).longValue())
                .toList();
        assertThat(familyRecipients).containsExactlyInAnyOrder(primary, withUrgent);
        assertThat(notificationOf(id, "FAMILY_MEMBER", primary).get("q354_case"))
                .isEqualTo("PRIMARY_CONTACT_EMERGENCY");
    }

    @Test
    void FR_014_aboutResident_outOfConsent_withoutCase_notSent_urgentFlagged_staffStillSent() {
        long member = TestUsers.nextId();
        family.relate(resident, relation(member, true, false, Set.of()));
        TestUser nurse = users.staff(Role.NURSING_STAFF);
        assignTo(nurse, resident);
        List<RecipientGroup> groups = List.of(
                new RecipientGroup.PrimaryContact(resident), new RecipientGroup.AssignedNurse(resident, DEFAULT));

        long medium = send(request(
                "fr014b-" + resident,
                NotificationLevel.MEDIUM,
                groups,
                Optional.empty(),
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.empty()));
        long urgent = send(request(
                "fr014c-" + resident,
                NotificationLevel.URGENT,
                groups,
                Optional.empty(),
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.empty()));

        assertThat(notifications(medium))
                .extracting(n -> n.get("recipient_type"))
                .containsExactly("STAFF");
        assertThat(flags(medium)).doesNotContain("MISSING_Q354_CASE");
        assertThat(notifications(urgent))
                .extracting(n -> n.get("recipient_type"))
                .containsExactly("STAFF");
        assertThat(flags(urgent)).contains("MISSING_Q354_CASE");
    }

    @Test
    void resolvedAtSendTime_assignmentEndedBeforeRequest_notRecipient() {
        TestUser before = users.staff(Role.NURSING_STAFF);
        TestUser now = users.staff(Role.NURSING_STAFF);
        assignments.assign(
                before.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        resident,
                        null,
                        time.now().minus(Duration.ofHours(10)),
                        time.now().minus(Duration.ofHours(2))));
        assignTo(now, resident);

        long id = send(staffRequest(
                "br04b-" + resident, NotificationLevel.MEDIUM, new RecipientGroup.AssignedNurse(resident, DEFAULT)));

        assertThat(staffRecipients(id)).containsExactly(now.staffId());
    }
}
