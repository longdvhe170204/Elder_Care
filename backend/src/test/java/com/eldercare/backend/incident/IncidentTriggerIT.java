package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * Trigger của bảng {@code incident} (T032, T034; R-15): nội dung ghi nhận không {@code UPDATE}, không xóa; Đã đóng chỉ
 * sang Đã hủy hoặc đổi loại (đính chính, V007_10); Đã hủy là trạng thái cuối.
 */
class IncidentTriggerIT extends IncidentHttpTestBase {

    @Test
    void T032_contentColumnsAreImmutable() {
        long id = incident("FALL", "MEDIUM", "NEW", null, nurse.accountId());
        for (String change : new String[] {
            "description = 'Sửa'",
            "resident_id = resident_id + 1",
            "initial_level = 'LOW'",
            "source_alert_id = 1",
            "discovered_at = NOW(6)",
            "recorded_by = recorded_by + 1"
        }) {
            assertThatThrownBy(() -> jdbc.update("UPDATE incident SET " + change + " WHERE id = ?", id))
                    .as(change)
                    .isInstanceOf(DataAccessException.class);
        }
        assertThatThrownBy(() -> jdbc.update("DELETE FROM incident WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }
}
