package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.CreateResult;
import org.junit.jupiter.api.Test;

/**
 * 001 T014 (US1 #2, #3, #10; DBR-01, FR-003, FR-004): CCCD duy nhất trong hồ sơ chưa ở trạng thái cuối; trùng hồ sơ Kết
 * thúc lưu trú, Hủy tiếp nhận thì tạo và liên kết hồ sơ gần nhất; trùng hồ sơ Qua đời thì tạo, không liên kết, kèm cảnh
 * báo.
 */
class ProfileCitizenIdIT extends ResidentProfileTestBase {

    @Test
    void US1_2_citizenIdOfOpenProfile_isRejectedWithExistingProfileCode() {
        String cccd = uniqueCitizenId();
        CreateResult existing = createAsOffice(withCitizenId(uniqueName(), cccd));

        assertThatThrownBy(() -> profiles.create(withCitizenId(uniqueName(), cccd), false))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.errorCode()).isEqualTo("CITIZEN_ID_IN_USE");
                    assertThat(e.properties())
                            .containsEntry("profileCode", existing.profile().profileCode());
                });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM resident WHERE id_document_number = ?", Long.class, cccd))
                .isEqualTo(1);
    }
}
