package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.ProfessionalActivityApi;
import com.cambae.backend.m15.account.api.dto.FailedLayer;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.LicenseView;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.PermissionResult;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.LicenseValidity;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeFacilityLicenses;
import com.cambae.backend.support.fake.m15.FakeLicenses;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T062 (US5 #4; FR-037 (b), CHK011, BR-M15-19): phạm vi hành nghề đối chiếu theo mục của danh mục phạm vi hành nghề
 * (008): giấy phép phải có mọi mục mà nghiệp vụ yêu cầu, dù còn hạn. Mục yêu cầu gắn tạm vào mục mặc định "chẩn đoán và
 * kê đơn nội bộ" rồi gỡ sau mỗi test (danh mục dùng chung với test khác).
 */
@Import(FakeM15Config.class)
class PracticeScopeIT extends MySqlIntegrationTestBase {

    private static final PermissionKey PRESCRIBE = new PermissionKey(Function.PRESCRIPTION, Operation.PROFESSIONAL);

    @Autowired
    PermissionApi permissions;

    @Autowired
    FakeLicenses licenses;

    @Autowired
    FakeFacilityLicenses facility;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    long internalMedicine;

    @BeforeEach
    void activityRequiresInternalMedicine() {
        LegalConditionIT.configureDefaults(jdbc);
        facility.scope(
                ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING,
                time.today().plusYears(1));
        internalMedicine = AccountFixtures.nextId();
        jdbc.update(
                "INSERT INTO professional_activity_practice_scope (activity_id, practice_scope_id)"
                        + " SELECT id, ? FROM professional_activity WHERE code = ?",
                internalMedicine,
                ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING);
    }

    @AfterEach
    void removeRequirement() {
        jdbc.update("DELETE FROM professional_activity_practice_scope WHERE practice_scope_id = ?", internalMedicine);
    }

    @Test
    void US5_4_licenseWithoutRequiredScope_denied_withScope_allowed() {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long without = fixtures.staffAccount("AC-03");
        long with = fixtures.staffAccount("AC-03");
        licenses.license(
                fixtures.staffId(without),
                LegalConditionIT.LICENSE_TYPE,
                new LicenseValidity(
                        AccountFixtures.nextId(),
                        "Giấy phép hành nghề",
                        time.today().plusYears(1),
                        Set.of(1L)));
        licenses.license(
                fixtures.staffId(with),
                LegalConditionIT.LICENSE_TYPE,
                new LicenseValidity(
                        AccountFixtures.nextId(),
                        "Giấy phép hành nghề",
                        time.today().plusYears(1),
                        Set.of(1L, internalMedicine)));

        PermissionResult denied = permissions.hasPermission(fixtures.user(without), PRESCRIBE, Target.UNSPECIFIED);
        assertThat(denied.failedLayer()).contains(FailedLayer.LEGAL);
        assertThat(denied.license())
                .contains(new LicenseView("Giấy phép hành nghề", time.today().plusYears(1)));
        assertThat(permissions
                        .hasPermission(fixtures.user(with), PRESCRIBE, Target.UNSPECIFIED)
                        .allowed())
                .isTrue();
    }
}
