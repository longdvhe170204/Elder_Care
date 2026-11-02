package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.FacilityLicenseApi;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.FacilityContactApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Bản thật nguồn giấy phép cho 002 và số liên hệ cho 009, không thay nghiệm thu lệnh quản lý. */
@Transactional
class FacilitySourcesIT extends MySqlIntegrationTestBase {
    @Autowired
    FacilityLicenseApi licenses;

    @Autowired
    FacilityContactApi contact;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    BusinessTime time;

    @Test
    void FR_071_licenseRequiresScopeMedicalFlagAndInclusiveIssueAndExpiryDates() {
        var today = time.today();
        String scope = "PA-" + TestUsers.nextId();
        addLicense(scope, today, today.plusDays(1), true, null, false);
        assertThat(licenses.coversScope(scope, today.minusDays(1))).isFalse();
        assertThat(licenses.coversScope(scope, today)).isTrue();
        assertThat(licenses.coversScope(scope, today.plusDays(1))).isTrue();
        assertThat(licenses.coversScope(scope, today.plusDays(2))).isFalse();
        assertThat(licenses.coversScope(scope + "-OTHER", today)).isFalse();
        String nonMedical = "PA-" + TestUsers.nextId();
        addLicense(nonMedical, today, today.plusDays(1), false, null, false);
        assertThat(licenses.coversScope(nonMedical, today)).isFalse();
    }

    @Test
    void FR_075_errorReplacementExcludesTheOldLicenseWithoutChangingItsRow() {
        var today = time.today();
        String scope = "PA-" + TestUsers.nextId();
        long original = addLicense(scope, today.minusDays(1), today.plusDays(2), true, null, false);
        String replacementScope = "PA-" + TestUsers.nextId();
        long replacement = addLicense(replacementScope, today, today.plusDays(2), true, original, true);
        assertThat(licenses.coversScope(scope, today)).isFalse();
        assertThat(licenses.coversScope(replacementScope, today)).isTrue();
        assertThat(licenses.expiringWithin(Period.ofDays(2)))
                .extracting(row -> row.licenseId())
                .contains(replacement)
                .doesNotContain(original);
        assertThat(catalogs.require(SharedKind.of("M06.FACILITY_LICENSE", Map.class), original)
                        .attributes())
                .containsEntry("medicalCare", true);
    }

    @Test
    void FR_039_expiringLicensesExcludeExpiredAndNotYetIssuedOnes() {
        var today = time.today();
        String scope = "PA-" + TestUsers.nextId();
        long expiresToday = addLicense(scope, today.minusDays(1), today, true, null, false);
        long expired = addLicense(scope, today.minusDays(2), today.minusDays(1), true, null, false);
        long future = addLicense(scope, today.plusDays(1), today.plusDays(2), true, null, false);
        assertThat(licenses.expiringWithin(Period.ofDays(2)))
                .extracting(row -> row.licenseId())
                .contains(expiresToday)
                .doesNotContain(expired, future);
    }

    @Test
    void FR_070_contactUsesTheConfiguredCatalogRowAndItsVersion() {
        var row = catalogs.create(
                SharedKind.of("M06.FACILITY_CONTACT", Map.class).notDeletable(),
                new NewCatalogItem<>(
                        "CURRENT",
                        "Số liên hệ viện",
                        null,
                        0,
                        Map.of("phoneNumber", "0123456789", "note", "Số kiểm thử")));
        assertThat(contact.current().phoneNumber()).isEqualTo("0123456789");
        assertThat(contact.current().note()).isEqualTo("Số kiểm thử");
        assertThat(contact.current().version()).isEqualTo(row.version());
    }

    private long addLicense(
            String scope, LocalDate issued, LocalDate expiry, boolean medical, Long replaced, boolean error) {
        long suffix = TestUsers.nextId();
        var attributes = new java.util.HashMap<String, Object>();
        attributes.put("number", "TEST-" + suffix);
        attributes.put("issuingAuthority", "Cơ quan kiểm thử");
        attributes.put("issueDate", issued.toString());
        attributes.put("expiryDate", expiry.toString());
        attributes.put("medicalCare", medical);
        attributes.put("operationScopes", Set.of(scope));
        attributes.put("replacesId", replaced);
        attributes.put("replacementDueToError", error);
        return catalogs.create(
                        SharedKind.of("M06.FACILITY_LICENSE", Map.class).notDeletable(),
                        new NewCatalogItem<>("TEST_" + suffix, "Giấy phép kiểm thử " + suffix, null, 0, attributes))
                .id();
    }
}
