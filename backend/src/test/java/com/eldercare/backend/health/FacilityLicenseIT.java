package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.spi.FacilityLicenseApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.FacilityContactApi;
import com.eldercare.backend.health.internal.license.service.FacilityLicenseService;
import com.eldercare.backend.health.internal.license.service.FacilityLicenseService.LicenseInput;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-070, FR-071, FR-075 (US9): chỉ Quản lý viện lưu giấy phép; giấy phép là bản mới, bản bị thay do sai sót không
 * còn dùng để xét hiệu lực; số liên hệ đổi có kiểm phiên bản.
 */
@Transactional
class FacilityLicenseIT extends VitalSignMeasurementTestBase {
    @Autowired
    FacilityLicenseService service;

    @Autowired
    FacilityLicenseApi licenseApi;

    @Autowired
    FacilityContactApi contactApi;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    private LocalDate today() {
        return time.today();
    }

    private void actAs(Role role) {
        asMeasurementUser(new TestUsers(jdbc, time).staff(role).user());
    }

    private LicenseInput input(String scope, boolean medical, Long replaces, boolean due, String reason) {
        return new LicenseInput(
                "GP-" + TestUsers.nextId(),
                "Sở Y tế",
                today().minusDays(1),
                today().plusDays(30),
                medical,
                List.of(scope),
                null,
                replaces,
                due,
                reason);
    }

    @Test
    void FR_070_onlyManagerSavesLicense() {
        actAs(Role.DOCTOR);

        assertThatThrownBy(() -> service.save(input("PA-1", true, null, false, null), today()))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_071_savedLicenseCoversScopeOnlyWithMedicalFlag() {
        actAs(Role.NURSING_HOME_MANAGER);
        String scope = "PA-" + TestUsers.nextId();
        String other = "PA-" + TestUsers.nextId();

        service.save(input(scope, true, null, false, null), today());
        service.save(input(other, false, null, false, null), today());

        assertThat(licenseApi.coversScope(scope, today())).isTrue();
        assertThat(licenseApi.coversScope(other, today())).isFalse();
    }

    @Test
    void FR_075_replacementForErrorKeepsOldRowButStopsUsingIt() {
        actAs(Role.NURSING_HOME_MANAGER);
        String oldScope = "PA-" + TestUsers.nextId();
        var old = service.save(input(oldScope, true, null, false, null), today());

        var replacement =
                service.save(input("PA-" + TestUsers.nextId(), true, old.id(), true, "Nhập nhầm ngày"), today());

        assertThat(licenseApi.coversScope(oldScope, today())).isFalse();
        var all = service.list(today());
        assertThat(all).extracting(v -> v.id()).contains(old.id(), replacement.id());
        assertThat(all.stream()
                        .filter(v -> v.id() == old.id())
                        .findFirst()
                        .orElseThrow()
                        .active())
                .isFalse();
    }

    @Test
    void FR_075_replacementForErrorNeedsReasonAndTarget() {
        actAs(Role.NURSING_HOME_MANAGER);

        assertThatThrownBy(() -> service.save(input("PA-1", true, null, true, "Lý do"), today()))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> service.save(input("PA-1", true, 1L, true, " "), today()))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_070_licenseDatesMustBeOrdered() {
        actAs(Role.NURSING_HOME_MANAGER);
        var bad = new LicenseInput(
                "GP-1", "Sở Y tế", today(), today().minusDays(1), true, List.of(), null, null, false, null);

        assertThatThrownBy(() -> service.save(bad, today())).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_070_contactChangeRequiresManagerAndIsReadBack() {
        actAs(Role.DOCTOR);
        assertThatThrownBy(() -> service.changeContact("0900000000", null, 0))
                .isInstanceOf(PermissionDeniedException.class);

        actAs(Role.NURSING_HOME_MANAGER);
        var saved = service.changeContact("0912345678", "Trực 24/7", 0);

        assertThat(contactApi.current().phoneNumber()).isEqualTo("0912345678");
        assertThat(saved.version()).isGreaterThanOrEqualTo(0);
    }
}
