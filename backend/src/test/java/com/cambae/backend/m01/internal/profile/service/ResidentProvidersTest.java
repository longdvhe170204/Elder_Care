package com.cambae.backend.m01.internal.profile.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import org.junit.jupiter.api.Test;

/**
 * 001 T011 (data-model mục 10): phân loại trường và gắn phạm vi của hồ sơ người cao tuổi cho 000 và 002. Phần các loại
 * đối tượng còn lại làm cùng bảng của từng user story.
 */
class ResidentProvidersTest {

    @Test
    void FR_049_residentFields_classifiedByInformationGroup() {
        var groups = new ResidentFieldClassification().fieldGroups();

        assertThat(new ResidentFieldClassification().entityType()).isEqualTo("M01.RESIDENT");
        assertThat(groups.get("idDocumentNumber")).isEqualTo(InfoGroup.IDENTITY);
        assertThat(groups.get("photoFileId")).isEqualTo(InfoGroup.IDENTITY);
        assertThat(groups.get("permanentAddress")).isEqualTo(InfoGroup.CONTACT);
        assertThat(groups.get("contact")).isEqualTo(InfoGroup.CONTACT);
        assertThat(groups.get("currentCareLevel")).isEqualTo(InfoGroup.CARE_LEVEL);
        assertThat(groups).doesNotContainKeys("fullName", "dateOfBirth", "gender", "status");
    }

    @Test
    void FR_034_residentScope_carriesResidentIdAndNoFloorArea() {
        Target target = new ResidentScopeAssignment().assignScope(42);

        assertThat(new ResidentScopeAssignment().entityType()).isEqualTo("M01.RESIDENT");
        assertThat(target.type()).isEqualTo("M01.RESIDENT");
        assertThat(target.id()).isEqualTo("42");
        assertThat(target.residentId()).isEqualTo(42L);
        assertThat(target.floorAreaId()).isNull();
    }
}
