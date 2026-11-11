package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipView;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.notification.api.spi.FamilyNotificationPort;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private RelationshipFixtures fixtures;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        // DATETIME(6) làm tròn phần dưới micro giây
        start = time.now().minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.MICROS);
        fixtures = new RelationshipFixtures(jdbc, tx, history, start);
    }

    @Test
    void F_14_familyRelationshipApi_readsActiveRelationshipsRolesAndRights() {
        long resident = TestUsers.nextId();
        long other = TestUsers.nextId();
        long r = fixtures.familyMember("Nguyễn Văn R", "0901000001");
        long relR = fixtures.relate(r, resident, 1, true, true, FamilyRight.VIEW_COSTS);
        long relOther = fixtures.relate(r, other, 1, false, false);
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
        assertThat(representatives.activeRepresentative(resident))
                .as("017 FR-034")
                .contains(r);
        assertThat(representatives.activeRepresentative(other)).isEmpty();
        assertThat(relationships.rightEnabled(r, resident, FamilyRight.VIEW_COSTS, now))
                .isTrue();
        assertThat(relationships.rightEnabled(r, resident, FamilyRight.VIEW_HEALTH, now))
                .as("FR-008: quyền mặc định tắt")
                .isFalse();
        // 007 T063, 012 T038: cổng người thân khẩn cấp của 007 đọc quan hệ thật (liên hệ chính, người đại diện theo thứ
        // tự).
        assertThat(emergency.primaryContact(resident)).hasValueSatisfying(c -> {
            assertThat(c.familyMemberId()).isEqualTo(r);
            assertThat(c.phoneNumber()).contains("0901000001");
        });
        assertThat(emergency.representatives(resident))
                .extracting(c -> c.familyMemberId())
                .containsExactly(r);
        assertThat(emergency.primaryContact(other)).isEmpty();
    }
}
