package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.AssignmentKind;
import com.cambae.backend.m15.account.api.AssignmentView;
import com.cambae.backend.m15.account.api.FailedLayer;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.LicenseValidity;
import com.cambae.backend.m15.account.api.LicenseView;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.PermissionResult;
import com.cambae.backend.m15.account.api.ProfessionalActivityApi;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.auth.internal.ActiveSession;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeFacilityLicenses;
import com.cambae.backend.support.fake.m15.FakeLicenses;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T061 (US5 #1 → #6; FR-037, FR-038, FR-040, BR-M06-05; research A-09): thao tác {@code PROFESSIONAL} kiểm giấy
 * phép người hành nghề và phạm vi hoạt động của cơ sở theo ngày (Asia/Ho_Chi_Minh) ở mỗi lần kiểm, không có job bật,
 * tắt; giấy phép còn hiệu lực tới hết ngày hết hạn; gia hạn có hiệu lực ngay; thao tác không chuyên môn không bị giới
 * hạn; bị từ chối thì kèm tên giấy phép, ngày hết hạn. US5 #8 (bản ghi đã xác nhận giữ nguyên) đúng vì chỉ kiểm trong
 * lệnh. Hai mục mặc định được cấu hình loại giấy phép {@code PRACTICE_LICENSE} như Quản lý viện sẽ làm.
 */
@Import(FakeM15Config.class)
class LegalConditionIT extends MySqlIntegrationTestBase {

    static final String LICENSE_TYPE = "PRACTICE_LICENSE";
    private static final PermissionKey PRESCRIBE = new PermissionKey(Function.PRESCRIPTION, Operation.PROFESSIONAL);
    private static final PermissionKey ADMINISTER =
            new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.PROFESSIONAL);

    @Autowired
    PermissionApi permissions;

    @Autowired
    FakeLicenses licenses;

    @Autowired
    FakeFacilityLicenses facility;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    LocalDate expiry;

    @BeforeEach
    void licenseTypeConfigured() {
        configureDefaults(jdbc);
        fixtures = new AccountFixtures(jdbc, time);
        expiry = time.today().plusDays(10);
    }

    /** Quản lý viện đã cấu hình loại giấy phép của hai mục mặc định. */
    static void configureDefaults(JdbcTemplate jdbc) {
        jdbc.update(
                "UPDATE professional_activity SET required_license_type = ? WHERE code IN (?, ?)",
                LICENSE_TYPE,
                ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING,
                ProfessionalActivityApi.MEDICATION_ADMINISTRATION);
    }

    @Test
    void US5_1_2_doctorLicenseValidThroughExpiryDay_deniedNextDay_withLicenseNameAndDate() {
        facility.scope(ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING, expiry.plusDays(100));
        ActiveSession doctor = doctorWithLicense(expiry);

        assertThat(permissions
                        .hasPermissionAt(doctor, PRESCRIBE, Target.UNSPECIFIED, lastMinuteOf(expiry))
                        .allowed())
                .isTrue();
        PermissionResult nextDay =
                permissions.hasPermissionAt(doctor, PRESCRIBE, Target.UNSPECIFIED, firstMinuteOf(expiry.plusDays(1)));
        assertThat(nextDay.failedLayer()).contains(FailedLayer.LEGAL);
        assertThat(nextDay.license()).contains(new LicenseView("Giấy phép hành nghề", expiry));
    }

    @Test
    void US5_3_facilityScopeExpired_doctorDenied() {
        facility.scope(ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING, expiry.minusDays(5));
        ActiveSession doctor = doctorWithLicense(expiry.plusDays(100));

        assertThat(permissions
                        .hasPermissionAt(doctor, PRESCRIBE, Target.UNSPECIFIED, firstMinuteOf(expiry))
                        .failedLayer())
                .contains(FailedLayer.LEGAL);
    }

    @Test
    void US5_5_nurseExpiredLicense_doseDenied_nonProfessionalStillAllowed() {
        long account = fixtures.staffAccount("AC-04");
        long staffId = fixtures.staffId(account);
        long floor = AccountFixtures.nextId();
        assignments.assign(
                staffId,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(30)),
                        null));
        licenses.license(
                staffId,
                LICENSE_TYPE,
                new LicenseValidity(
                        AccountFixtures.nextId(),
                        "Giấy phép hành nghề",
                        time.today().minusDays(1),
                        Set.of()));
        ActiveSession nurse = fixtures.user(account);
        Target onFloor = new Target("M07.DOSE", "1", null, floor);

        assertThat(permissions.hasPermission(nurse, ADMINISTER, onFloor).failedLayer())
                .contains(FailedLayer.LEGAL);
        assertThat(permissions
                        .hasPermission(nurse, new PermissionKey(Function.ABSENCE_RETURN, Operation.VIEW), onFloor)
                        .allowed())
                .isTrue();
    }

    @Test
    void US5_6_renewedLicense_allowedAgain_withoutManagerAction() {
        facility.scope(ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING, expiry.plusDays(400));
        long account = fixtures.staffAccount("AC-03");
        long staffId = fixtures.staffId(account);
        licenses.license(
                staffId,
                LICENSE_TYPE,
                new LicenseValidity(
                        AccountFixtures.nextId(),
                        "Giấy phép hành nghề",
                        time.today().minusDays(1),
                        Set.of()));
        ActiveSession doctor = fixtures.user(account);
        assertThat(permissions
                        .hasPermission(doctor, PRESCRIBE, Target.UNSPECIFIED)
                        .allowed())
                .isFalse();

        licenses.license(
                staffId,
                LICENSE_TYPE,
                new LicenseValidity(
                        AccountFixtures.nextId(),
                        "Giấy phép hành nghề",
                        time.today().plusYears(5),
                        Set.of()));

        assertThat(permissions
                        .hasPermission(doctor, PRESCRIBE, Target.UNSPECIFIED)
                        .allowed())
                .isTrue();
    }

    @Test
    void BR_M06_05_noLicenseAtAll_denied_legal() {
        facility.scope(ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING, expiry.plusDays(100));
        ActiveSession doctor = fixtures.user(fixtures.staffAccount("AC-03"));

        PermissionResult result = permissions.hasPermission(doctor, PRESCRIBE, Target.UNSPECIFIED);
        assertThat(result.failedLayer()).contains(FailedLayer.LEGAL);
        assertThat(result.license()).isEmpty();
    }

    private ActiveSession doctorWithLicense(LocalDate expiryDate) {
        long account = fixtures.staffAccount("AC-03");
        licenses.license(
                fixtures.staffId(account),
                LICENSE_TYPE,
                new LicenseValidity(AccountFixtures.nextId(), "Giấy phép hành nghề", expiryDate, Set.of()));
        return fixtures.user(account);
    }

    private Instant lastMinuteOf(LocalDate date) {
        return time.startOfDay(date.plusDays(1)).minus(Duration.ofMinutes(1));
    }

    private Instant firstMinuteOf(LocalDate date) {
        return time.startOfDay(date).plus(Duration.ofMinutes(1));
    }
}
