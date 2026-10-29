package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.resident.internal.consent.domain.Consent;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T054 (BR-M01-08, DBR-03, FR-015; US5 Independent Test): điều kiện chú thích ¹ kiểm ở mỗi yêu cầu, không đệm. Bản
 * đồng ý gồm người thân X mà không gồm Y thì X xem được sức khỏe, Y không; rút lại hoặc thay bản mới thì quyền đổi ngay
 * từ yêu cầu kế tiếp. Kiểm cả qua {@link PermissionApi} thật của 002 (dòng "Dị ứng, bệnh nền" của AC-10).
 */
class BR_M01_08_ConsentConditionIT extends ConsentTestBase {

    private static final PermissionKey VIEW_HEALTH = new PermissionKey(Function.ALLERGY_CONDITION, Operation.VIEW);

    @Autowired
    PermissionApi permissions;

    @Autowired
    List<FootnoteCondition> conditions;

    private boolean canViewHealth(TestUser family, long residentId) {
        PermissionResult result = permissions.hasPermission(family.user(), VIEW_HEALTH, target(residentId));
        return result.allowed();
    }

    @Test
    void BR_M01_08_footnote1_isImplementedByM01() {
        assertThat(conditions).extracting(FootnoteCondition::footnoteCode).contains("1");
    }

    @Test
    void BR_M01_08_familyInScope_seesHealth_otherRelativeDoesNot() {
        long residentId = newResident();
        long x = relatedFamilyMember(residentId);
        long y = relatedFamilyMember(residentId);
        TestUser familyX = users.family(x);
        TestUser familyY = users.family(y);

        assertThat(canViewHealth(familyX, residentId)).as("chưa có bản đồng ý").isFalse();

        record(residentId, byResident(Set.of(x)));

        assertThat(canViewHealth(familyX, residentId)).isTrue();
        assertThat(canViewHealth(familyY, residentId)).isFalse();
    }

    @Test
    void FR_015_withdrawal_removesTheAccessFromTheNextRequest() {
        long residentId = newResident();
        long x = relatedFamilyMember(residentId);
        TestUser familyX = users.family(x);
        Consent consent = record(residentId, byResident(Set.of(x)));
        assertThat(canViewHealth(familyX, residentId)).isTrue();

        user.set(office.user());
        consents.withdraw(consents.targetOf(consent.id()), consent.id(), new Reason("Rút lại"), evidence());

        assertThat(canViewHealth(familyX, residentId)).isFalse();
    }

    @Test
    void FR_013_newConsentReplacesTheScope() {
        long residentId = newResident();
        long x = relatedFamilyMember(residentId);
        long y = relatedFamilyMember(residentId);
        TestUser familyX = users.family(x);
        TestUser familyY = users.family(y);
        record(residentId, byResident(Set.of(x)));

        record(residentId, byResident(Set.of(y)));

        assertThat(canViewHealth(familyX, residentId)).isFalse();
        assertThat(canViewHealth(familyY, residentId)).isTrue();
    }

    @Test
    void BR_M01_08_footnote1_onlyFamilyAccountsAndOnlyTargetsWithAResident() {
        long residentId = newResident();
        long x = relatedFamilyMember(residentId);
        record(residentId, byResident(Set.of(x)));
        TestUser familyX = users.family(x);
        FootnoteCondition condition = conditions.stream()
                .filter(c -> c.footnoteCode().equals("1"))
                .findFirst()
                .orElseThrow();

        assertThat(condition.isSatisfied(familyX.user(), target(residentId), Instant.now()))
                .isTrue();
        assertThat(condition.isSatisfied(office.user(), target(residentId), Instant.now()))
                .isFalse();
        assertThat(condition.isSatisfied(
                        familyX.user(),
                        com.eldercare.backend.administration.account.api.dto.Target.UNSPECIFIED,
                        Instant.now()))
                .isFalse();
    }
}
