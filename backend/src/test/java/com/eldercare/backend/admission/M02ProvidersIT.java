package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T011: provider cho 000, 002. Giá, cọc thuộc nhóm tài chính, nguyên nhân qua đời thuộc nhóm sức khỏe; lượt vắng, hồ sơ kết thúc,
 * bản ghi qua đời gắn đúng người cao tuổi.
 */
@AutoConfigureMockMvc
@WithMockUser
class M02ProvidersIT extends DeathTestBase {

    @Autowired
    List<FieldClassificationProvider> classifications;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    private FieldClassificationProvider classification(String type) {
        return classifications.stream()
                .filter(p -> type.equals(p.entityType()))
                .findFirst()
                .orElseThrow();
    }

    private ScopeAssignmentProvider scope(String type) {
        return scopes.stream()
                .filter(p -> type.equals(p.entityType()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void T011_financeAndHealthFieldsAreClassified_otherFieldsStayInternalPublic() {
        assertThat(classification("M02.CONTRACT").fieldGroups())
                .containsEntry("depositAmount", InfoGroup.FINANCE)
                .containsEntry("recordedUnitPrices", InfoGroup.FINANCE)
                .doesNotContainKey("status");
        assertThat(classification("M02.PRICE_VERSION").fieldGroups()).containsEntry("unitPrice", InfoGroup.FINANCE);
        assertThat(classification("M02.DEATH_RECORD").fieldGroups()).containsEntry("cause", InfoGroup.HEALTH);
        assertThat(classification("M02.ADMISSION_APPLICATION").fieldGroups())
                .as("ý kiến vào ở là công khai nội bộ")
                .isEmpty();
    }

    @Test
    void T011_scopeOfAbsenceEndingAndDeath_isTheResident() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(3), 10, 0));
        recordReturn(residentId, null);
        long endingId = createEnding(residentId, d0.plusDays(10));
        doctorRecordsDeath(residentId, null);
        long recordId = jdbc.queryForObject(
                "SELECT id FROM residency_ending WHERE kind = 'DEATH' AND resident_id = ?", Long.class, residentId);

        assertThat(scope("M02.ABSENCE").assignScope(absenceId).residentId()).isEqualTo(residentId);
        assertThat(scope("M02.RESIDENCY_ENDING").assignScope(endingId).residentId())
                .isEqualTo(residentId);
        assertThat(scope("M02.DEATH_RECORD").assignScope(recordId).residentId()).isEqualTo(residentId);
    }
}
