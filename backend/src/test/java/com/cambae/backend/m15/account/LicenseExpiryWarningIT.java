package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.events.LicenseExpiring;
import com.cambae.backend.m15.account.api.spi.ExpiringFacilityLicense;
import com.cambae.backend.m15.account.api.spi.ExpiringLicense;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeFacilityLicenses;
import com.cambae.backend.support.fake.m15.FakeLicenses;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * 002 T062 (US5 #7; FR-039, research A-09): job hằng ngày cảnh báo giấy phép cơ sở và giấy phép người hành nghề sắp hết
 * hạn trong CFG-M06-02, một lần cho mỗi {@code (loại, giấy phép, ngày hết hạn)}; gia hạn đổi ngày hết hạn nên lần sau
 * lại cảnh báo; cảnh báo nêu nghiệp vụ chuyên môn sẽ bị tắt và người liên quan.
 */
@Import(FakeM15Config.class)
@RecordApplicationEvents
class LicenseExpiryWarningIT extends MySqlIntegrationTestBase {

    @Autowired
    List<ScheduledJob> jobs;

    @Autowired
    FakeLicenses licenses;

    @Autowired
    FakeFacilityLicenses facility;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    void configured() {
        LegalConditionIT.configureDefaults(jdbc);
    }

    @Test
    void FR_039_eachLicenseAndExpiryWarnedOnce_renewalWarnsAgain() {
        long practitionerLicense = AccountFixtures.nextId();
        long staffId = AccountFixtures.nextId();
        long facilityLicense = AccountFixtures.nextId();
        licenses.expiring(new ExpiringLicense(
                practitionerLicense,
                staffId,
                LegalConditionIT.LICENSE_TYPE,
                "Giấy phép hành nghề",
                time.today().plusDays(30)));
        facility.expiring(new ExpiringFacilityLicense(
                facilityLicense, "Giấy phép hoạt động", time.today().plusDays(20)));

        job().runNow();
        job().runNow();

        assertThat(warnings(practitionerLicense)).singleElement().satisfies(e -> {
            assertThat(e.licenseKind()).isEqualTo("PRACTITIONER");
            assertThat(e.relatedStaffIds()).containsExactly(staffId);
            assertThat(e.affectedActivities()).contains("Chẩn đoán và kê đơn nội bộ", "Phát thuốc và xác nhận liều");
        });
        assertThat(warnings(facilityLicense)).singleElement().satisfies(e -> {
            assertThat(e.licenseKind()).isEqualTo("FACILITY");
            assertThat(e.affectedActivities()).containsExactly("Chẩn đoán và kê đơn nội bộ");
        });

        facility.expiring(new ExpiringFacilityLicense(
                facilityLicense, "Giấy phép hoạt động", time.today().plusDays(50)));
        job().runNow();

        assertThat(warnings(facilityLicense)).hasSize(2);
    }

    private ScheduledJob job() {
        return jobs.stream()
                .filter(j -> j.name().equals("WARN_EXPIRING_LICENSES"))
                .findFirst()
                .orElseThrow();
    }

    private List<LicenseExpiring> warnings(long licenseId) {
        return events.stream(LicenseExpiring.class)
                .filter(e -> e.licenseId() == licenseId)
                .toList();
    }
}
