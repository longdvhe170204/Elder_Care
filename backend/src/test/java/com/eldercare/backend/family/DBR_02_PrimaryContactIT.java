package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

/**
 * 012 T009 (US1; DBR-02, FR-003 → FR-006, FR-012; quickstart Q2 → Q4): đúng một liên hệ chính ({@code UNIQUE}), ít nhất
 * một người đại diện với người cao tuổi đang ở viện; Đang tiếp nhận lưu được khi chưa đủ nhưng Hoàn tất tiếp nhận bị chặn
 * điều kiện {@code DBR_02}; kết thúc quan hệ xét lại tài khoản trong lệnh và bỏ khỏi danh sách đón.
 */
class DBR_02_PrimaryContactIT extends FamilyCommandTestBase {

    @Autowired
    FamilyRightsApi rights;

    @Autowired
    RepresentativePort representatives;

    @Test
    void DBR_02_atMostOnePrimaryContact() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long r = relate(a, member("Trần R"), true, true);
        long m = member("Trần M");

        createRelationship(a, m, true, false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DBR_02_VIOLATED"));

        long second = relate(a, m, false, false);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE family_relationship SET primary_contact = TRUE WHERE id = ?", second))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(column(r, "primary_contact")).isTrue();
    }

    private List<Condition> completeAdmissionConditions(long residentId) {
        return familyParticipant()
                .checkConditions(new CommandContext(
                        residentId,
                        ResidentStatus.ADMISSION_IN_PROGRESS,
                        StatusCommand.COMPLETE_ADMISSION,
                        null,
                        time.now(),
                        null));
    }
}
