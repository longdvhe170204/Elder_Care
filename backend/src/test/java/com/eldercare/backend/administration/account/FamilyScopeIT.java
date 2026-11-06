package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.PermissionMatrixIT.resident;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.RelationshipFixtures;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeResidencyEnds;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 002 T067 (US6 #1, #6, #7; FR-045, FR-049; SC-012): phạm vi của người thân là người cao tuổi có quan hệ Hiệu lực;
 * quan hệ chấm dứt thì ra khỏi phạm vi từ lần xem kế tiếp; người cao tuổi ở trạng thái cuối còn trong phạm vi ở chế độ
 * chỉ đọc tới hết CFG-M01-04 (mặc định 30 ngày), sau đó ra khỏi phạm vi.
 */
@Import(FakeM15Config.class)
class FamilyScopeIT extends MySqlIntegrationTestBase {

    private static final PermissionKey PROFILE_VIEW = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);
    private static final PermissionKey FEEDBACK_CREATE = new PermissionKey(Function.FEEDBACK, Operation.CREATE);

    @Autowired
    PermissionApi permissions;

    @Autowired
    ScopeApi scope;

    @Autowired
    RelationshipHistoryStore history;

    @Autowired
    TransactionTemplate tx;

    RelationshipFixtures family;

    @Autowired
    FakeResidencyEnds residencyEnds;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    long familyMember;
    ActiveSession user;

    @BeforeEach
    void setUp() {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        family = new RelationshipFixtures(
                jdbc, tx, history, time.now().minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.MICROS));
        familyMember = family.familyMember("Người thân thử", "0909000000");
        user = fixtures.familyUser(fixtures.familyAccount(familyMember));
    }

    @Test
    void US6_1_seesOnlyRelatedResident() {
        long a = AccountFixtures.nextId();
        long b = AccountFixtures.nextId();
        family.relate(familyMember, a, 1, false, false);

        assertThat(permissions.hasPermission(user, PROFILE_VIEW, resident(a)).allowed())
                .isTrue();
        PermissionResult other = permissions.hasPermission(user, PROFILE_VIEW, resident(b));
        assertThat(other.allowed()).isFalse();
        assertThat(other.failedLayer()).contains(FailedLayer.SCOPE);
        assertThat(scope.filter(user, PROFILE_VIEW)).isEqualTo(new ScopeFilter(false, Set.of(), Set.of(), Set.of(a)));
    }

    @Test
    void FR_049_relationshipEnded_outOfScopeNextView() {
        long a = AccountFixtures.nextId();
        long relationship = family.relate(familyMember, a, 1, false, false);
        assertThat(permissions.hasPermission(user, PROFILE_VIEW, resident(a)).allowed())
                .isTrue();

        family.end(relationship, time.now());

        assertThat(permissions.hasPermission(user, PROFILE_VIEW, resident(a)).allowed())
                .isFalse();
        assertThat(scope.filter(user, PROFILE_VIEW).residentIds()).isEmpty();
    }

    @Test
    void US6_7_residentEndedWithinGrace_readOnly() {
        long a = AccountFixtures.nextId();
        family.relate(familyMember, a, 1, false, false);
        residencyEnds.ended(a, time.now().minus(Duration.ofDays(29)));

        assertThat(permissions.hasPermission(user, PROFILE_VIEW, resident(a)).allowed())
                .isTrue();
        PermissionResult write = permissions.hasPermission(user, FEEDBACK_CREATE, resident(a));
        assertThat(write.allowed()).isFalse();
        assertThat(write.failedLayer()).contains(FailedLayer.SCOPE);
        assertThat(scope.filter(user, PROFILE_VIEW).residentIds()).containsExactly(a);
    }

    @Test
    void US6_6_residentEndedPastGrace_outOfScope_otherResidentStays() {
        long ended = AccountFixtures.nextId();
        long staying = AccountFixtures.nextId();
        family.relate(familyMember, ended, 1, false, false);
        family.relate(familyMember, staying, 1, false, false);
        residencyEnds.ended(ended, time.now().minus(Duration.ofDays(31)));

        assertThat(permissions
                        .hasPermission(user, PROFILE_VIEW, resident(ended))
                        .allowed())
                .isFalse();
        assertThat(permissions
                        .hasPermission(user, PROFILE_VIEW, resident(staying))
                        .allowed())
                .isTrue();
        assertThat(permissions
                        .hasPermission(user, FEEDBACK_CREATE, resident(staying))
                        .allowed())
                .isTrue();
        assertThat(scope.filter(user, PROFILE_VIEW).residentIds()).containsExactly(staying);
    }
}
