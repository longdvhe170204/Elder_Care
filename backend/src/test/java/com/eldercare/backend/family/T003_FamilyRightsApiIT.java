package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.family.api.dto.IdDocumentView;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.resident.FakeConsentApi;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 012 T003, T006 (contracts mục 1.2; research F-04, F-14): {@code QuyenNguoiThanApi} cho 013, 014, 017, 018 đọc quan
 * hệ, quyền có tác dụng (bản đồng ý, FR-011), người đại diện, liên hệ chính, danh sách đón tại thời điểm, giấy tờ.
 */
@Import(FakeConsentApi.Config.class)
class T003_FamilyRightsApiIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    RelationshipHistoryStore history;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    FamilyRightsApi rights;

    @Autowired
    FakeConsentApi consents;

    private Instant start;
    private RelationshipFixtures fixtures;

    @BeforeEach
    void setUp() {
        // DATETIME(6) làm tròn phần dưới micro giây
        start = time.now().minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.MICROS);
        fixtures = new RelationshipFixtures(jdbc, tx, history, start);
    }

    @AfterEach
    void resetConsents() {
        consents.reset();
    }

    @Test
    void FR_011_viewHealth_isEffectiveOnlyWhenTheConsentIncludesTheFamilyMember() {
        long resident = TestUsers.nextId();
        long m = fixtures.familyMember("Nguyễn Thị M", "0902000001");
        long n = fixtures.familyMember("Nguyễn Văn N", "0902000002");
        fixtures.relate(m, resident, 1, true, true, FamilyRight.VIEW_HEALTH, FamilyRight.VIEW_COSTS);
        fixtures.relate(n, resident, 2, false, false, FamilyRight.VIEW_HEALTH);
        consents.active(resident, Set.of(m), false);
        Instant now = time.now();

        assertThat(rights.rightEffective(m, resident, FamilyRight.VIEW_HEALTH, now))
                .isTrue();
        assertThat(rights.rightEffective(n, resident, FamilyRight.VIEW_HEALTH, now))
                .as("DBR-03: quyền đang bật nhưng bản đồng ý không bao gồm N")
                .isFalse();
        assertThat(rights.rightEffective(n, resident, FamilyRight.VIEW_COSTS, now))
                .as("FR-008: quyền mặc định tắt")
                .isFalse();
        assertThat(rights.rightEffective(m, resident, FamilyRight.VIEW_COSTS, now))
                .isTrue();

        consents.withdraw(resident);
        assertThat(rights.rightEffective(m, resident, FamilyRight.VIEW_HEALTH, now))
                .as("rút bản đồng ý: quyền giữ giá trị, hết tác dụng")
                .isFalse();
        assertThat(rights.rightEffective(m, resident, FamilyRight.VIEW_COSTS, now))
                .isTrue();
    }

    @Test
    void F_14_relationshipRepresentativeAndPrimaryContact() {
        long resident = TestUsers.nextId();
        long rep = fixtures.familyMember("Trần Văn R", "0902000003");
        long primary = fixtures.familyMember("Trần Thị C", "0902000004");
        long rep2 = fixtures.familyMember("Trần Văn D", "0902000005");
        long ended = fixtures.familyMember("Trần Thị E", "0902000006");
        fixtures.relate(rep, resident, 2, true, false);
        fixtures.relate(primary, resident, 1, false, true);
        fixtures.relate(rep2, resident, 3, true, false);
        long endedRelationship = fixtures.relate(ended, resident, 4, true, false);
        fixtures.end(endedRelationship, start.plus(Duration.ofDays(1)));
        Instant now = time.now();

        assertThat(rights.hasActiveRelationship(rep, resident)).isTrue();
        assertThat(rights.hasActiveRelationship(ended, resident)).isFalse();
        assertThat(rights.isRepresentative(rep, resident, now)).isTrue();
        assertThat(rights.isRepresentative(primary, resident, now)).isFalse();
        assertThat(rights.isRepresentative(ended, resident, now)).isFalse();
        assertThat(rights.isRepresentative(ended, resident, start.plusSeconds(60)))
                .as("FR-068: tại thời điểm quan hệ còn Hiệu lực")
                .isTrue();
        assertThat(rights.representatives(resident))
                .as("theo thứ tự liên hệ (FR-012)")
                .extracting(FamilyMemberView::familyMemberId)
                .containsExactly(rep, rep2);
        assertThat(rights.primaryContact(resident)).hasValueSatisfying(c -> {
            assertThat(c.familyMemberId()).isEqualTo(primary);
            assertThat(c.fullName()).isEqualTo("Trần Thị C");
            assertThat(c.phoneNumber()).isEqualTo("0902000004");
        });
        assertThat(rights.primaryContact(TestUsers.nextId())).isEmpty();
    }

    @Test
    void FR_016_allowedPickersAt_followThePickupRightHistory() {
        long resident = TestUsers.nextId();
        long rep = fixtures.familyMember("Lê Văn R", "0902000007");
        long x = fixtures.familyMember("Lê Thị X", "0902000008");
        long none = fixtures.familyMember("Lê Văn K", "0902000009");
        long relRep = fixtures.relate(rep, resident, 1, true, true, FamilyRight.PICKUP_ALLOWED);
        fixtures.relate(x, resident, 2, false, false, FamilyRight.PICKUP_ALLOWED);
        fixtures.relate(none, resident, 3, false, false);
        Instant removed = start.plus(Duration.ofDays(2));
        fixtures.record(relRep, RelationshipAttribute.PICKUP_ALLOWED, false, removed);

        assertThat(rights.allowedPickersAt(resident, start.plusSeconds(60)))
                .extracting(FamilyMemberView::familyMemberId)
                .containsExactly(rep, x);
        assertThat(rights.allowedPickersAt(resident, removed.plusSeconds(1)))
                .as("người đại diện bị bỏ khỏi danh sách đón vẫn là người đại diện (FR-025)")
                .extracting(FamilyMemberView::familyMemberId)
                .containsExactly(x);
        assertThat(rights.allowedPickersAt(resident, start.minusSeconds(1))).isEmpty();
    }

    @Test
    void FR_046b_belongings_representativeOrPickupAllowed() {
        long resident = TestUsers.nextId();
        long rep = fixtures.familyMember("Phạm Văn R", "0902000010");
        long picker = fixtures.familyMember("Phạm Thị P", "0902000011");
        long other = fixtures.familyMember("Phạm Văn O", "0902000012");
        fixtures.relate(rep, resident, 1, true, true);
        fixtures.relate(picker, resident, 2, false, false, FamilyRight.PICKUP_ALLOWED);
        fixtures.relate(other, resident, 3, false, false, FamilyRight.VIEW_COSTS);
        Instant now = time.now();

        assertThat(rights.canViewBelongings(rep, resident, now)).isTrue();
        assertThat(rights.canViewBelongings(picker, resident, now)).isTrue();
        assertThat(rights.canViewBelongings(other, resident, now)).isFalse();
        assertThat(rights.canViewBelongings(other, TestUsers.nextId(), now)).isFalse();
    }

    @Test
    void F_14_idDocument_forRecipientVerificationOf013() {
        long withDocument = fixtures.familyMember("Võ Văn G", "0902000013", "CCCD", "079123456789");
        long without = fixtures.familyMember("Võ Thị H", "0902000014");

        assertThat(rights.idDocument(withDocument)).contains(new IdDocumentView("CCCD", "079123456789"));
        assertThat(rights.idDocument(without)).isEmpty();
        assertThat(rights.idDocument(TestUsers.nextId())).isEmpty();
    }
}
