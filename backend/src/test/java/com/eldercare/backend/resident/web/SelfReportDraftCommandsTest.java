package com.eldercare.backend.resident.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 001 T116 (Q-412, BR-M01-18): lệnh client thấy trên hồ sơ. Hành chính có ô T⁵² nên ma trận cho cả lệnh ghi phiếu, lập
 * đánh giá; hồ sơ chỉ đưa lệnh lập bản Nháp tự khai khi người cao tuổi còn Đang tiếp nhận; Bác sĩ, Điều dưỡng không có
 * lệnh lập Nháp (kiểm chứng dùng chính lệnh ghi phiếu).
 */
class SelfReportDraftCommandsTest {

    private static Set<String> granted() {
        return new HashSet<>(Set.of(
                "RECORD_END_OF_LIFE_WISH",
                "RECORD_TREATMENT_WISH",
                "CREATE_ASSESSMENT",
                "DRAFT_END_OF_LIFE_WISH",
                "DRAFT_TREATMENT_WISH",
                "RECORD_FAMILY_DECLARED"));
    }

    @Test
    void office_whileAdmitting_getsDraftCommandsOnly() {
        Set<String> commands = granted();
        ResidentController.applySelfReportDrafts(commands, true, ResidentStatus.ADMISSION_IN_PROGRESS);
        assertThat(commands)
                .containsExactlyInAnyOrder("DRAFT_END_OF_LIFE_WISH", "DRAFT_TREATMENT_WISH", "RECORD_FAMILY_DECLARED");
    }

    @Test
    void office_afterAdmission_getsNone() {
        Set<String> commands = granted();
        ResidentController.applySelfReportDrafts(commands, true, ResidentStatus.IN_RESIDENCE);
        assertThat(commands).isEmpty();
    }

    @Test
    void clinician_keepsRecordCommands_withoutDraftCommands() {
        Set<String> commands = granted();
        ResidentController.applySelfReportDrafts(commands, false, ResidentStatus.ADMISSION_IN_PROGRESS);
        assertThat(commands)
                .containsExactlyInAnyOrder("RECORD_END_OF_LIFE_WISH", "RECORD_TREATMENT_WISH", "CREATE_ASSESSMENT");
    }
}
