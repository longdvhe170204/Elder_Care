package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.resident.ConsentTestBase;
import com.eldercare.backend.resident.internal.consent.domain.Consent;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 002 T067 (US6 #3, Independent Test; BR-M01-08, DBR-03, FR-045, FR-046; SC-012): hai người thân của cùng người cao
 * tuổi, chỉ một người trong bản đồng ý; phần sức khỏe (ô có chú thích ¹ và nhóm trường sức khỏe, thuốc, nguyện vọng)
 * hiện với người này, ẩn với người kia; điều kiện kiểm ở mỗi lần xem nên rút đồng ý có hiệu lực ngay. Bản đồng ý ghi,
 * rút bằng lệnh thật của 001 (điều kiện chú thích "1" là {@code ConsentFootnoteCondition}). T100: quyền riêng của người thân
 * ({@code VIEW_HEALTH}, {@code VIEW_COSTS}; FR-045 (3)) cũng phải bật.
 */
class FamilyHealthIT extends ConsentTestBase {

    private static final PermissionKey VITAL_SIGN_VIEW = new PermissionKey(Function.VITAL_SIGN, Operation.VIEW);
    private static final PermissionKey PROFILE_VIEW = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);
    private static final PermissionKey COST_VIEW = new PermissionKey(Function.COST_ADJUSTMENT, Operation.VIEW);

    @Autowired
    PermissionApi permissions;

    @Autowired
    FieldVisibilityApi visibility;

    AccountFixtures fixtures;
    long resident;
    Target target;
    long includedMember;
    Consent consent;
    ActiveSession included;
    ActiveSession notIncluded;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        resident = newResident();
        target = PermissionMatrixIT.resident(resident);
        includedMember = relatedFamilyMember(resident);
        long otherMember = relatedFamilyMember(resident);
        consent = record(resident, byResident(Set.of(includedMember)));
        included = fixtures.familyUser(fixtures.familyAccount(includedMember));
        notIncluded = fixtures.familyUser(fixtures.familyAccount(otherMember));
    }

    @Test
    void FR_046_healthOnlyForFamilyMemberInConsent() {
        assertThat(permissions.hasPermission(included, VITAL_SIGN_VIEW, target).allowed())
                .isTrue();
        PermissionResult other = permissions.hasPermission(notIncluded, VITAL_SIGN_VIEW, target);
        assertThat(other.allowed()).isFalse();
        assertThat(other.failedLayer()).contains(FailedLayer.ROLE);

        // Hồ sơ không có chú thích ¹: cả hai xem được, chỉ phần sức khỏe khác nhau
        assertThat(permissions.hasPermission(notIncluded, PROFILE_VIEW, target).allowed())
                .isTrue();
        for (InfoGroup group : new InfoGroup[] {InfoGroup.HEALTH, InfoGroup.MEDICATION, InfoGroup.WISHES}) {
            assertThat(visibility.canView(included, group, target))
                    .as(group.name())
                    .isEqualTo(Visibility.FULL);
            assertThat(visibility.canView(notIncluded, group, target))
                    .as(group.name())
                    .isEqualTo(Visibility.NONE);
        }
        assertThat(visibility.canView(notIncluded, InfoGroup.COSTS, target)).isEqualTo(Visibility.FULL);
    }

    @Test
    void BR_M01_08_withdrawnConsent_hiddenFromNextView() {
        assertThat(visibility.canView(included, InfoGroup.HEALTH, target)).isEqualTo(Visibility.FULL);

        user.set(office.user());
        consents.withdraw(
                consents.targetOf(consent.id()), consent.id(), new Reason("Người cao tuổi rút lại"), evidence());

        assertThat(permissions.hasPermission(included, VITAL_SIGN_VIEW, target).allowed())
                .isFalse();
        assertThat(visibility.canView(included, InfoGroup.HEALTH, target)).isEqualTo(Visibility.NONE);
    }

    @Test
    void T100_viewHealthRightOff_healthHiddenDespiteConsent() {
        families.disable(includedMember, resident, FamilyRight.VIEW_HEALTH);

        PermissionResult vital = permissions.hasPermission(included, VITAL_SIGN_VIEW, target);
        assertThat(vital.allowed()).isFalse();
        assertThat(vital.failedLayer()).contains(FailedLayer.ROLE);
        for (InfoGroup group : new InfoGroup[] {InfoGroup.HEALTH, InfoGroup.MEDICATION, InfoGroup.WISHES}) {
            assertThat(visibility.canView(included, group, target))
                    .as(group.name())
                    .isEqualTo(Visibility.NONE);
        }
        assertThat(permissions.hasPermission(included, PROFILE_VIEW, target).allowed())
                .isTrue();
    }

    @Test
    void T100_viewCostsRight_decidesCosts() {
        assertThat(permissions.hasPermission(included, COST_VIEW, target).allowed())
                .isTrue();
        assertThat(visibility.canView(included, InfoGroup.COSTS, target)).isEqualTo(Visibility.FULL);

        families.disable(includedMember, resident, FamilyRight.VIEW_COSTS);

        PermissionResult costs = permissions.hasPermission(included, COST_VIEW, target);
        assertThat(costs.allowed()).isFalse();
        assertThat(costs.failedLayer()).contains(FailedLayer.ROLE);
        assertThat(visibility.canView(included, InfoGroup.COSTS, target)).isEqualTo(Visibility.NONE);
        assertThat(visibility.canView(included, InfoGroup.HEALTH, target)).isEqualTo(Visibility.FULL);
    }

    @Test
    void DBR_03_consentForAnotherResident_doesNotCount() {
        long other = newResident();
        families.relate(includedMember, other);

        assertThat(permissions
                        .hasPermission(included, VITAL_SIGN_VIEW, PermissionMatrixIT.resident(other))
                        .allowed())
                .isFalse();
        assertThat(visibility.canView(included, InfoGroup.HEALTH, PermissionMatrixIT.resident(other)))
                .isEqualTo(Visibility.NONE);
    }
}
