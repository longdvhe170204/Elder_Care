package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.LicenseView;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.LicenseValidity;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeFacilityLicenses;
import com.eldercare.backend.support.fake.administration.FakeLicenses;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
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
                "UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.requiredLicenseType', ?)"
                        + " WHERE catalog_type = 'M15.PROFESSIONAL_ACTIVITY' AND code IN (?, ?)",
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
    void FR_072_examTreatment_needsDoctorLicenseAndFacilityScope() {
        jdbc.update(
                "UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.requiredLicenseType', ?)"
                        + " WHERE catalog_type = 'M15.PROFESSIONAL_ACTIVITY' AND code = ?",
                LICENSE_TYPE,
                ProfessionalActivityApi.INTERNAL_EXAM_TREATMENT);
        var examine = new PermissionKey(Function.EXAM_TREATMENT, Operation.PROFESSIONAL);
        ActiveSession doctor = doctorWithLicense(expiry);

        assertThat(permissions
                        .hasPermissionAt(doctor, examine, Target.UNSPECIFIED, lastMinuteOf(expiry))
                        .failedLayer())
                .as("giấy phép cơ sở chưa có phạm vi khám và điều trị")
                .contains(FailedLayer.LEGAL);
        facility.scope(ProfessionalActivityApi.INTERNAL_EXAM_TREATMENT, expiry.plusDays(100));
        assertThat(permissions
                        .hasPermissionAt(doctor, examine, Target.UNSPECIFIED, lastMinuteOf(expiry))
                        .allowed())
                .isTrue();
        assertThat(permissions
                        .hasPermissionAt(doctor, examine, Target.UNSPECIFIED, firstMinuteOf(expiry.plusDays(1)))
                        .failedLayer())
                .as("giấy phép bác sĩ hết hạn")
                .contains(FailedLayer.LEGAL);
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
