package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void T032_closedIncidentOnlyMovesToCancelledOrChangesType() {
        long id = incident("FALL", "MEDIUM", "CLOSED", nurse.accountId(), nurse.accountId());
        long other = typeId("FACILITY");

        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET status = 'IN_PROGRESS' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET level = 'LOW' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.update(
                        "UPDATE incident SET current_type_id = ?, type_code = 'FACILITY', version = version + 1"
                                + " WHERE id = ?",
                        other,
                        id))
                .isEqualTo(1);
        assertThat(jdbc.update("UPDATE incident SET status = 'CANCELLED' WHERE id = ?", id))
                .isEqualTo(1);
    }

    @Test
    void T032_cancelledIncidentIsFinal() {
        long id = incident("FALL", "LOW", "CLOSED", nurse.accountId(), nurse.accountId());
        jdbc.update("UPDATE incident SET status = 'CANCELLED' WHERE id = ?", id);

        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET status = 'CLOSED' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET type_code = 'FACILITY' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void T034_openEmergencyKeyIsGeneratedAndNotUnique() {
        long first = incident("FALL", "CRITICAL", "NEW", null, nurse.accountId());
        long second = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM incident WHERE resident_id = ? AND open_emergency = 1",
                        Integer.class,
                        resident))
                .isEqualTo(2);
        jdbc.update("UPDATE incident SET status = 'IN_PROGRESS' WHERE id = ?", first);
        jdbc.update("UPDATE incident SET status = 'CLOSED' WHERE id IN (?, ?)", first, second);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM incident WHERE resident_id = ? AND open_emergency = 1",
                        Integer.class,
                        resident))
                .isZero();
    }
}
