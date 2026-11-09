package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T062 (US8; FR-069, FR-070, FR-071, BR-M02-21): phần của 004 trong lệnh Ghi nhận qua đời của 001. Bản ghi qua đời được
 * lập, hợp đồng Chấm dứt "Qua đời", lượt vắng đóng tại thời điểm qua đời, hồ sơ kết thúc đang chuẩn bị chuyển Đã hủy, năm mục
 * việc sau qua đời được tạo; lệnh không bị chặn bởi đồ gửi, thuốc gửi, chi phí hay sự cố; Hành chính ghi nhận phải có bằng
 * chứng (001 kiểm lúc nhận lệnh).
 */
@AutoConfigureMockMvc
@WithMockUser
class RecordDeathEffectsIT extends DeathTestBase {

    private Map<String, Object> deathRecord(long residentId) {
        return jdbc.queryForMap("SELECT * FROM residency_ending WHERE kind = 'DEATH' AND resident_id = ?", residentId);
    }

    private String taskStatus(long recordId, String code) {
        return jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(s.data, CONCAT('$.postDeathItems.', ?, '.status')))"
                        + " FROM residency_ending d JOIN resident_module_state s"
                        + " ON s.resident_id = d.resident_id AND s.module = 'M02' WHERE d.id = ?",
                String.class,
                code,
                recordId);
    }

    @Test
    void FR_070_openItemsDoNotBlockTheCommand() throws Exception {
        long residentId = inResidence();
        allItems(residentId, PostDeathItemStatus.NOT_COMPLETED);

        doctorRecordsDeath(residentId, null);

        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.DECEASED);
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_LENGTH(JSON_SEARCH(data, 'all', 'NOT_COMPLETED', NULL, '$.postDeathItems.*.status'))"
                                + " FROM resident_module_state WHERE resident_id = ? AND module = 'M02'",
                        Integer.class,
                        residentId))
                .isEqualTo(5);
        assertThat(residencyClosed(residentId)).isFalse();
    }
}
