package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * BR-M05-08, DBR-19 (T065): bản ghi sự cố khẩn cấp không sửa, không xóa; sửa sai qua đính chính (đã kiểm ở
 * {@code BR_M05_21_IncidentCorrectionIT}). Chặn bằng quyền cột của tài khoản ứng dụng (R-14) và trigger
 * {@code trg_incident_row_lock}, {@code trg_incident_no_delete}.
 */
class BR_M05_08_EmergencyIncidentImmutableIT extends IncidentHttpTestBase {
    @Test
    void BR_M05_08_emergencyIncidentContentCannotBeUpdated() {
        long id = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET description = 'Sửa tay' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageMatching("(?s).*(incident content is immutable|UPDATE command denied).*");
        assertThat(jdbc.queryForObject("SELECT description FROM incident WHERE id = ?", String.class, id))
                .isEqualTo("Mô tả sự cố");
    }

    @Test
    void BR_M05_08_emergencyIncidentCannotBeDeleted() {
        long id = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        assertThatThrownBy(() -> jdbc.update("DELETE FROM incident WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM incident WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
    }
}
