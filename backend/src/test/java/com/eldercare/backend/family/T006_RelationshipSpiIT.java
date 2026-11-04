package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipView;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.incident.api.dto.EmergencyFamilyContactView;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.notification.api.spi.FamilyNotificationPort;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 012 T006 (contracts mục 1.1; research F-04, F-14): bản cài thật các SPI module khác khai báo cho 012, đọc quan hệ,
 * vai trò, quyền ở bảng của 012 và lịch sử quyền tại thời điểm (FR-068). Dữ liệu lập thẳng bằng SQL và lịch sử như lệnh
 * của US1 sẽ ghi (T011).
 */
class T006_RelationshipSpiIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    RelationshipHistoryStore history;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    FamilyRelationshipApi relationships;

    @Autowired
    RepresentativePort representatives;

    @Autowired
    FamilyNotificationPort notifications;

    @Autowired
    EmergencyFamilyPort emergency;

    @Autowired
    List<FootnoteCondition> footnotes;

    private TestUsers users;
    private Instant start;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        // DATETIME(6) làm tròn phần dưới micro giây
        start = time.now().minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.MICROS);
    }

    private long familyMember(String name, String phone) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO family_member (full_name, phone_number, created_by, created_at)"
                                    + " VALUES (?, ?, 1, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setString(1, name);
                    s.setString(2, phone);
                    s.setObject(3, utc(start));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Quan hệ Hiệu lực từ {@code start}; vai trò, quyền ghi cả cột và lịch sử như lệnh của US1. */
    private long relate(
            long familyMemberId,
            long residentId,
            int order,
            boolean representative,
            boolean primaryContact,
            FamilyRight... rights) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        List<FamilyRight> enabled = List.of(rights);
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO family_relationship (family_member_id, resident_id, relationship,"
                                    + " representative, primary_contact, contact_order, view_health, view_costs,"
                                    + " register_visits, receive_urgent, pickup_allowed, request_services, started_at,"
                                    + " created_by, created_at) VALUES (?, ?, 'CHILD', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, familyMemberId);
                    s.setLong(2, residentId);
                    s.setBoolean(3, representative);
                    s.setBoolean(4, primaryContact);
                    s.setInt(5, order);
                    int i = 6;
                    for (FamilyRight right : FamilyRight.values()) {
                        s.setBoolean(i++, enabled.contains(right));
                    }
                    s.setObject(12, utc(start));
                    s.setObject(13, utc(start));
                    return s;
                },
                keys);
        long id = keys.getKey().longValue();
        tx.executeWithoutResult(s -> {
            if (representative) {
                history.record(id, RelationshipAttribute.REPRESENTATIVE, true, start, "CREATE", 1L, null);
            }
            for (FamilyRight right : enabled) {
                history.record(id, RelationshipAttribute.of(right), true, start, "CREATE", 1L, null);
            }
        });
        return id;
    }

    private void end(long relationshipId, Instant at) {
        jdbc.update(
                "UPDATE family_relationship SET status = 'ENDED', ended_at = ?, end_reason = 'test' WHERE id = ?",
                utc(at),
                relationshipId);
    }

    @Test
    void F_14_familyRelationshipApi_readsActiveRelationshipsRolesAndRights() {
        long resident = TestUsers.nextId();
        long other = TestUsers.nextId();
        long r = familyMember("Nguyễn Văn R", "0901000001");
        long relR = relate(r, resident, 1, true, true, FamilyRight.VIEW_COSTS);
        long relOther = relate(r, other, 1, false, false);
        Instant now = time.now();

        assertThat(relationships.active(r))
                .extracting(FamilyRelationshipView::relationshipId)
                .containsExactlyInAnyOrder(relR, relOther);
        assertThat(relationships.belongsToResident(r, resident)).isTrue();
        assertThat(relationships.relationship(r, resident)).contains(FamilyRelationshipType.CHILD);
        assertThat(relationships.registeredPhoneNumber(r)).contains("0901000001");
        assertThat(relationships.familyMemberName(r)).contains("Nguyễn Văn R");
        assertThat(relationships.isActiveRepresentative(r, resident, now)).isTrue();
        assertThat(relationships.isActiveRepresentative(r, other, now)).isFalse();
        assertThat(relationships.rightEnabled(r, resident, FamilyRight.VIEW_COSTS, now))
                .isTrue();
        assertThat(relationships.rightEnabled(r, resident, FamilyRight.VIEW_HEALTH, now))
                .as("FR-008: quyền mặc định tắt")
                .isFalse();
    }

    @Test
    void FR_068_rightsAndRepresentative_areEvaluatedAtTheGivenTime() {
        long resident = TestUsers.nextId();
        long m = familyMember("Trần Thị M", "0901000002");
        long rel = relate(m, resident, 1, true, false, FamilyRight.PICKUP_ALLOWED);
        Instant turnedOff = start.plus(Duration.ofDays(3));
        tx.executeWithoutResult(
                s -> history.record(rel, RelationshipAttribute.PICKUP_ALLOWED, false, turnedOff, "REQUEST", 1L, null));
        Instant ended = start.plus(Duration.ofDays(5));
        end(rel, ended);

        assertThat(relationships.rightEnabled(m, resident, FamilyRight.PICKUP_ALLOWED, start.minusSeconds(1)))
                .as("trước khi lập quan hệ")
                .isFalse();
        assertThat(relationships.rightEnabled(m, resident, FamilyRight.PICKUP_ALLOWED, start.plusSeconds(60)))
                .isTrue();
        assertThat(relationships.rightEnabled(m, resident, FamilyRight.PICKUP_ALLOWED, turnedOff.plusSeconds(1)))
                .as("đã tắt qua yêu cầu")
                .isFalse();
        assertThat(relationships.isActiveRepresentative(m, resident, ended.minusSeconds(1)))
                .isTrue();
        assertThat(relationships.isActiveRepresentative(m, resident, ended))
                .as("FR-006: quan hệ đã kết thúc")
                .isFalse();
        assertThat(relationships.belongsToResident(m, resident)).isFalse();
        assertThat(relationships.active(m)).isEmpty();
    }

    @Test
    void FR_021_representativePort_forContractsAndResidencyChanges() {
        long resident = TestUsers.nextId();
        long noRepresentative = TestUsers.nextId();
        long r = familyMember("Lê Văn R", "0901000003");
        long n = familyMember("Lê Thị N", "0901000004");
        relate(r, resident, 1, true, true);
        relate(n, resident, 2, false, false);
        relate(n, noRepresentative, 1, false, true);

        assertThat(representatives.hasRepresentative(resident)).isTrue();
        assertThat(representatives.hasRepresentative(noRepresentative)).isFalse();
        assertThat(representatives.isActiveRepresentative(r, resident, time.today()))
                .isTrue();
        assertThat(representatives.isActiveRepresentative(n, resident, time.today()))
                .isFalse();
        assertThat(representatives.isActiveRepresentative(
                        r, resident, time.today().minusDays(20)))
                .as("trước khi lập quan hệ")
                .isFalse();
    }

    @Test
    void FR_012_notificationPort_rightsCallOrderAndContact() {
        long resident = TestUsers.nextId();
        long primary = familyMember("Phạm Văn C", "0901000005");
        long rep = familyMember("Phạm Thị D", "0901000006");
        long urgent = familyMember("Phạm Văn E", "0901000007");
        long none = familyMember("Phạm Thị F", "0901000008");
        relate(primary, resident, 1, false, true);
        relate(none, resident, 2, false, false, FamilyRight.VIEW_COSTS);
        relate(urgent, resident, 3, false, false, FamilyRight.RECEIVE_URGENT);
        relate(rep, resident, 4, true, false);
        long account = users.family(primary).accountId();

        Map<Long, NotifiableRelationshipView> byMember = notifications.activeRelationships(resident).stream()
                .collect(Collectors.toMap(NotifiableRelationshipView::familyMemberId, Function.identity()));
        assertThat(byMember.get(primary).rights())
                .as("Q-96: người liên hệ chính luôn nhận khẩn")
                .containsExactly(FamilyRight.RECEIVE_URGENT);
        assertThat(byMember.get(primary).portalAccountId()).contains(account);
        assertThat(byMember.get(primary).portalAccountActive()).isTrue();
        assertThat(byMember.get(primary).phoneNumber()).contains("0901000005");
        assertThat(byMember.get(rep).representative()).isTrue();
        assertThat(byMember.get(rep).portalAccountId()).isEmpty();
        assertThat(byMember.get(none).rights()).containsExactly(FamilyRight.VIEW_COSTS);

        assertThat(notifications.callOrder(resident))
                .as("009 FR-029a: người đại diện, rồi người có quyền nhận khẩn theo thứ tự liên hệ")
                .containsExactly(rep, primary, urgent);

        assertThat(notifications.contact(none)).hasValueSatisfying(c -> {
            assertThat(c.fullName()).isEqualTo("Phạm Thị F");
            assertThat(c.phoneNumber()).contains("0901000008");
            assertThat(c.portalAccountId()).isEmpty();
        });
        long unrelated = familyMember("Không quan hệ", "0901000009");
        assertThat(notifications.contact(unrelated))
                .as("kể cả khi không có quan hệ")
                .isPresent();
    }

    @Test
    void FR_047_emergencyPort_primaryContactAndRepresentativesInContactOrder() {
        long resident = TestUsers.nextId();
        long a = familyMember("Võ Văn A", "0901000010");
        long b = familyMember("Võ Thị B", "0901000011");
        long c = familyMember("Võ Văn C", "0901000012");
        relate(a, resident, 3, true, false);
        relate(b, resident, 1, true, true);
        relate(c, resident, 2, false, false);

        assertThat(emergency.primaryContact(resident))
                .map(EmergencyFamilyContactView::familyMemberId)
                .contains(b);
        assertThat(emergency.representatives(resident))
                .extracting(EmergencyFamilyContactView::familyMemberId)
                .containsExactly(b, a);
        assertThat(emergency.primaryContact(TestUsers.nextId())).isEmpty();
    }

    @Test
    void A_05_footnotes2And7_onlyActiveRepresentative() {
        long resident = TestUsers.nextId();
        long rep = familyMember("Đỗ Văn R", "0901000013");
        long other = familyMember("Đỗ Thị M", "0901000014");
        long outsider = familyMember("Đỗ Văn X", "0901000015");
        relate(rep, resident, 1, true, true);
        relate(other, resident, 2, false, false);
        relate(outsider, TestUsers.nextId(), 1, true, true);
        CurrentUser repUser = users.family(rep).user();
        CurrentUser otherUser = users.family(other).user();
        CurrentUser outsiderUser = users.family(outsider).user();
        Target target = new Target("M10.RELATIONSHIP", "1", resident, null);

        for (String code : List.of("2", "7")) {
            FootnoteCondition condition = footnotes.stream()
                    .filter(f -> f.footnoteCode().equals(code))
                    .findFirst()
                    .orElseThrow();
            assertThat(condition.isSatisfied(repUser, target, time.now())).isTrue();
            assertThat(condition.isSatisfied(otherUser, target, time.now())).isFalse();
            assertThat(condition.isSatisfied(outsiderUser, target, time.now()))
                    .as("ngoài phạm vi: lớp phạm vi của 002 trả 404 (FR-042)")
                    .isTrue();
            assertThat(condition.isSatisfied(repUser, new Target("X", "1", null, null), time.now()))
                    .isFalse();
        }
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
