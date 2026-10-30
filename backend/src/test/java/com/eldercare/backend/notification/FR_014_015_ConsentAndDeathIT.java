package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.notification.internal.quiethours.service.QuietHoursRelease;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.notification.FakeFamilyNotifications;
import com.eldercare.backend.support.fake.resident.FakeConsentApi;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidentEvents;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
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
 * 009 US2, US4 phần chờ 001 (T014, T029; tên logic {@code FR_013_LocNguoiThanIT}, {@code FR_014_NhanThongBaoDongYIT},
 * {@code FR_015_QuaDoiIT}): bản đồng ý Hiệu lực ({@code ConsentApi}) quyết định phạm vi "nhận thông báo" (FR-014, Q-353,
 * Q-357: mục chung, bật thì mọi người thân có quan hệ thuộc phạm vi) và phần sức khỏe (FR-013, DBR-03: người có quyền xem
 * sức khỏe và nằm trong bản đồng ý); người cao tuổi Qua đời ({@code ResidentApi}) chặn thông báo phát sinh sau thời điểm
 * qua đời trừ việc sau qua đời của 004 (FR-015); thông báo đang giữ xét lại bản đồng ý hiện hành và qua đời lúc giải
 * phóng (FR-025, US4 #6).
 */
@Import({FakeM15Config.class, FakeM01Config.class, FakeConsentApi.Config.class})
class FR_014_015_ConsentAndDeathIT extends M13IntegrationTestBase {

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeFamilyNotifications family;

    @Autowired
    FakeConsentApi consents;

    @Autowired
    FakeResidents residents;

    @Autowired
    FakeResidentEvents residentEvents;

    @Autowired
    QuietHoursRelease release;

    TestUsers users;
    long resident;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        resident = TestUsers.nextId();
        residents.resident(resident, "Nguyễn Văn A", Gender.MALE, ResidentStatus.IN_RESIDENCE);
    }

    @AfterEach
    void reset() {
        clock.reset();
        family.reset();
        consents.reset();
    }

    private static Instant at(String local) {
        return OffsetDateTime.parse(local + "+07:00").toInstant();
    }

    /** Người thân có quan hệ Hiệu lực, có tài khoản cổng Hoạt động. */
    private long relative(boolean primary, FamilyRight... rights) {
        long id = TestUsers.nextId();
        TestUser account = users.family(id);
        family.relate(
                resident,
                new NotifiableRelationshipView(
                        id, primary, false, Set.of(rights), Optional.of(account.accountId()), true, Optional.empty()));
        return id;
    }

    private NotificationRequest request(
            String key,
            NotificationLevel level,
            Instant occurredAt,
            boolean afterDeath,
            Optional<Q354Case> q354Case,
            List<RecipientGroup> groups,
            InformationType... parts) {
        return new NotificationRequest(
                "M10",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M10.TEST", key),
                key,
                Optional.of(level),
                Optional.of(resident),
                Optional.empty(),
                groups,
                List.of(parts).stream()
                        .map(t -> new ContentPart(t, "Nội dung " + t))
                        .toList(),
                Optional.of("Rút gọn"),
                Optional.of("Tiêu đề"),
                Optional.empty(),
                false,
                occurredAt,
                Map.of(),
                afterDeath,
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                q354Case);
    }

    /** Thông báo Nhẹ "về người cao tuổi" tới mọi người thân có quyền {@code right}. */
    private NotificationRequest aboutResident(String key, FamilyRight right, InformationType... parts) {
        return request(
                key,
                NotificationLevel.LOW,
                time.now(),
                false,
                Optional.empty(),
                List.of(new RecipientGroup.FamilyWithRight(resident, right)),
                parts);
    }

    private long send(NotificationRequest r) {
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        return ((Number) awaitRequests(r.eventKey(), 1).getFirst().get("id")).longValue();
    }

    /** Người thân nhận thông báo của yêu cầu, kèm phần được xem. */
    private Map<Long, String> familyRecipients(long requestId) {
        Map<Long, String> found = new java.util.TreeMap<>();
        jdbc.queryForList(
                        "SELECT recipient_id, visible_parts FROM notification WHERE request_id = ?"
                                + " AND recipient_type = 'FAMILY_MEMBER'",
                        requestId)
                .forEach(row -> found.put(
                        ((Number) row.get("recipient_id")).longValue(), String.valueOf(row.get("visible_parts"))));
        return found;
    }

    private Map<String, Object> state(long requestId, long familyMemberId) {
        return jdbc.queryForMap(
                "SELECT n.id, n.status FROM notification n WHERE n.request_id = ? AND n.recipient_type = 'FAMILY_MEMBER' AND n.recipient_id = ?",
                requestId,
                familyMemberId);
    }

    private String notSentReason(Map<String, Object> state) {
        return jdbc.queryForObject(
                "SELECT e.content->>'$.reason' FROM record_event e WHERE e.kind = 'M13.NOTIFICATION_EVENT' AND e.entity_type = 'M13.NOTIFICATION' AND e.entity_id = ?"
                        + " AND e.content->>'$.type' = 'NOT_SENT'",
                String.class,
                ((Number) state.get("id")).longValue());
    }

    @Test
    void FR_014_notificationConsentOn_everyRelativeInScope_offOrMissing_none() {
        long m = relative(true, FamilyRight.VIEW_COSTS);
        long r = relative(false, FamilyRight.VIEW_COSTS);

        long none = send(aboutResident("fr014-none-" + resident, FamilyRight.VIEW_COSTS, InformationType.GENERAL));
        consents.active(resident, Set.of(), false);
        long off = send(aboutResident("fr014-off-" + resident, FamilyRight.VIEW_COSTS, InformationType.GENERAL));
        consents.active(resident, Set.of(m), true);
        long on = send(aboutResident("fr014-on-" + resident, FamilyRight.VIEW_COSTS, InformationType.GENERAL));

        assertThat(familyRecipients(none)).isEmpty();
        assertThat(familyRecipients(off)).isEmpty();
        // Q-357: mục "nhận thông báo" là mục chung, R không cần nằm trong danh sách xem sức khỏe
        assertThat(familyRecipients(on)).containsOnlyKeys(m, r);
        assertThat(jdbc.queryForList("SELECT q354_case FROM notification WHERE request_id = ?", String.class, on))
                .containsOnlyNulls();
    }

    @Test
    void FR_013_healthPartOnlyForRelativeWithRightAndInConsent() {
        long m = relative(true, FamilyRight.VIEW_HEALTH);
        long n = relative(false);
        long p = relative(false, FamilyRight.VIEW_HEALTH);
        consents.active(resident, Set.of(m), true);

        long id = send(request(
                "fr013-" + resident,
                NotificationLevel.LOW,
                time.now(),
                false,
                Optional.empty(),
                List.of(
                        new RecipientGroup.FamilyWithRight(resident, FamilyRight.VIEW_HEALTH),
                        new RecipientGroup.PrimaryContact(resident),
                        new RecipientGroup.FamilyMember(n, "TEST")),
                InformationType.GENERAL,
                InformationType.HEALTH));

        Map<Long, String> got = familyRecipients(id);
        assertThat(got.get(m)).contains("GENERAL").contains("HEALTH");
        assertThat(got.get(n)).isEqualTo("GENERAL");
        assertThat(got.get(p)).isEqualTo("GENERAL");
    }

    @Test
    void FR_015_afterDeath_noFamilyNotification_staffStillNotified() {
        long m = relative(true, FamilyRight.VIEW_COSTS);
        consents.active(resident, Set.of(m), true);
        TestUser manager = users.staff(Role.NURSING_HOME_MANAGER);
        Instant death = time.now().minus(Duration.ofHours(1));
        residents.changeStatus(resident, ResidentStatus.DECEASED, death);

        long id = send(request(
                "fr015-after-" + resident,
                NotificationLevel.LOW,
                time.now(),
                false,
                Optional.empty(),
                List.of(
                        new RecipientGroup.PrimaryContact(resident),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                InformationType.GENERAL));

        assertThat(familyRecipients(id)).isEmpty();
        assertThat(jdbc.queryForList(
                        "SELECT recipient_id FROM notification WHERE request_id = ? AND recipient_type = 'STAFF'",
                        Long.class,
                        id))
                .contains(manager.staffId());
    }

    @Test
    void FR_015_deathNoticeAtDeathTime_andAfterDeathTask_stillReachFamily() {
        long m = relative(true);
        Instant death = time.now().minus(Duration.ofMinutes(5));
        residents.changeStatus(resident, ResidentStatus.DECEASED, death);

        long notice = send(request(
                "fr015-notice-" + resident,
                NotificationLevel.URGENT,
                death,
                false,
                Optional.of(Q354Case.DEATH),
                List.of(new RecipientGroup.PrimaryContact(resident)),
                InformationType.GENERAL));
        long task = send(request(
                "fr015-task-" + resident,
                NotificationLevel.URGENT,
                time.now(),
                true,
                Optional.of(Q354Case.DEATH),
                List.of(new RecipientGroup.PrimaryContact(resident)),
                InformationType.GENERAL));

        assertThat(familyRecipients(notice)).containsOnlyKeys(m);
        assertThat(familyRecipients(task)).containsOnlyKeys(m);
    }

    @Test
    void FR_025_heldNotice_residentDiesBeforeRelease_notSentResidentDeceased() {
        long n = relative(false, FamilyRight.VIEW_COSTS);
        consents.active(resident, Set.of(), true);
        clock.setTo(at("2030-03-04T22:30:00"));
        long id = send(aboutResident("fr025-death-" + resident, FamilyRight.VIEW_COSTS, InformationType.GENERAL));
        assertThat(state(id, n).get("status")).isEqualTo("HELD_FOR_QUIET_HOURS");

        clock.setTo(at("2030-03-05T05:00:00"));
        residents.changeStatus(resident, ResidentStatus.DECEASED, time.now());
        tx.executeWithoutResult(s -> residentEvents.statusChanged(
                resident,
                ResidentStatus.HOSPITALIZED,
                ResidentStatus.DECEASED,
                StatusCommand.RECORD_DEATH,
                time.now()));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> "NOT_SENT".equals(state(id, n).get("status")));
        assertThat(notSentReason(state(id, n))).isEqualTo("RESIDENT_DECEASED");
        clock.setTo(at("2030-03-05T07:00:00"));
        release.run();
        assertThat(state(id, n).get("status")).isEqualTo("NOT_SENT");
    }

    @Test
    void FR_025_heldNotice_releaseChecksDeathAndCurrentConsent() {
        long n = relative(false, FamilyRight.VIEW_COSTS);
        long other = TestUsers.nextId();
        consents.active(resident, Set.of(), true);
        clock.setTo(at("2030-03-04T22:30:00"));
        long dies = send(aboutResident("fr025-dies-" + resident, FamilyRight.VIEW_COSTS, InformationType.GENERAL));

        // Người cao tuổi thứ hai: bản đồng ý bị rút trước 07:00
        long first = resident;
        resident = other;
        residents.resident(resident, "Trần Thị B", Gender.FEMALE, ResidentStatus.IN_RESIDENCE);
        long r = relative(false, FamilyRight.VIEW_COSTS);
        consents.active(resident, Set.of(), true);
        long withdrawn =
                send(aboutResident("fr025-withdrawn-" + resident, FamilyRight.VIEW_COSTS, InformationType.GENERAL));

        clock.setTo(at("2030-03-05T05:00:00"));
        // Qua đời không kèm sự kiện: lúc giải phóng vẫn phải chặn (FR-025)
        residents.changeStatus(first, ResidentStatus.DECEASED, time.now());
        consents.withdraw(other);
        clock.setTo(at("2030-03-05T07:00:00"));
        release.run();

        assertThat(state(dies, n).get("status")).isEqualTo("NOT_SENT");
        assertThat(notSentReason(state(dies, n))).isEqualTo("RESIDENT_DECEASED");
        assertThat(state(withdrawn, r).get("status")).isEqualTo("NOT_SENT");
        assertThat(notSentReason(state(withdrawn, r))).isEqualTo("RECIPIENT_NO_LONGER_ELIGIBLE");
    }
}
