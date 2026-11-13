package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.health.internal.governance.service.HealthFieldClassifications;
import com.eldercare.backend.health.internal.governance.service.HealthFilePermissionProviders;
import com.eldercare.backend.health.internal.governance.service.HealthScopeAssignments;
import org.junit.jupiter.api.Test;

class HealthProvidersTest {

    @Test
    void FR_080_healthFieldsClassifiedAsHealthInfoGroup() {
        var vitalSign = HealthFieldClassifications.vitalSignRecord();
        assertThat(vitalSign.entityType()).isEqualTo("M06.VITAL_SIGN_RECORD");
        assertThat(vitalSign.fieldGroups().get("value")).isEqualTo(InfoGroup.HEALTH);
        assertThat(vitalSign.fieldGroups().get("level")).isEqualTo(InfoGroup.HEALTH);

        var exam = HealthFieldClassifications.examinationRecord();
        assertThat(exam.entityType()).isEqualTo("M06.EXAMINATION_RECORD");
        assertThat(exam.fieldGroups().get("content")).isEqualTo(InfoGroup.HEALTH);
        assertThat(exam.fieldGroups().get("clinicalAssessment")).isEqualTo(InfoGroup.HEALTH);
    }

    @Test
    void FR_034_healthScopeAssignment_carriesCorrectTarget() {
        var vitalSignScope = HealthScopeAssignments.vitalSignRecord();
        assertThat(vitalSignScope.entityType()).isEqualTo("M06.VITAL_SIGN_RECORD");
        var target = vitalSignScope.assignScope(101L);
        assertThat(target.type()).isEqualTo("M06.VITAL_SIGN_RECORD");
        assertThat(target.id()).isEqualTo("101");

        var licenseScope = HealthScopeAssignments.facilityLicense();
        assertThat(licenseScope.entityType()).isEqualTo("M06.FACILITY_LICENSE");
        var licenseTarget = licenseScope.assignScope(202L);
        assertThat(licenseTarget.type()).isEqualTo("M06.FACILITY_LICENSE");
        assertThat(licenseTarget.residentId()).isNull();
    }

    @Test
    void NFR_08_healthFilePermissions_registeredForLicenseAndExam() {
        var licenseFile = HealthFilePermissionProviders.facilityLicenseScan();
        assertThat(licenseFile.ownerType()).isEqualTo("M06.SCAN_GIAY_PHEP_CO_SO");
        assertThat(licenseFile.canDownload(1L)).isTrue();

        var examFile = HealthFilePermissionProviders.examinationFile();
        assertThat(examFile.ownerType()).isEqualTo("M06.TEP_KHAM");
        assertThat(examFile.canDownload(2L)).isTrue();
    }
}
