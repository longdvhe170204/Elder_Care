package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
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
                "SELECT g.outcome FROM notification_request r, JSON_TABLE(r.recipient_groups, '$[*]' COLUMNS"
                        + " (seq INT PATH '$.seq', outcome VARCHAR(40) PATH '$.outcome')) g WHERE r.id = ? ORDER BY g.seq",
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
                        "SELECT a.content->>'$.channel' FROM record_event a JOIN notification n ON n.id = a.entity_id"
                                + " WHERE a.kind = 'M13.DELIVERY_ATTEMPT' AND a.entity_type = 'M13.NOTIFICATION'"
                                + " AND n.request_id = ?",
                        String.class,
                        id))
                .isEqualTo("IN_APP");
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
}
