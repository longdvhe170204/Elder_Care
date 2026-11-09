package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.internal.critical.service.CriticalFlagStatusParticipant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

/**
 * DBR-34 và bảng trạng thái dấu nguy kịch (FR-050a): tối đa một dấu đang mở mỗi người, mỗi dấu đúng một cảnh báo; gỡ
 * dấu không đóng cảnh báo; ghi lại sau khi gỡ tạo dấu và cảnh báo mới; tự gỡ khi chuyển viện hoặc trạng thái cuối.
 */
class DBR_34_CriticalFlagIT extends CriticalCareTestBase {

    @Autowired
    CriticalFlagStatusParticipant participant;

    @Test
    void DBR_34_oneOpenFlagPerResident_oneAlertPerFlag() throws Exception {
        var created = flag(doctor);
        long flagId = created.id();
        long alertId = created.alertId();

        assertThat(flagStatus(flagId)).isEqualTo("OPEN");
        var alert = jdbc.queryForMap(
                "SELECT type, level, status, critical_flag_id, resident_id FROM alert WHERE id = ?", alertId);
        assertThat(alert.get("type")).isEqualTo("CRITICAL_CONDITION");
        assertThat(alert.get("level")).isEqualTo("CRITICAL");
        assertThat(alert.get("critical_flag_id")).isEqualTo(flagId);
        assertThat(alert.get("resident_id")).isEqualTo(resident);

        post(doctor, FLAGS, resident, "{\"type\":\"OFFICIAL\",\"assessment\":\"Lần hai\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CRITICAL_FLAG_EXISTS"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM alert WHERE critical_flag_id IS NOT NULL AND resident_id = ?",
                        Integer.class,
                        resident))
                .isEqualTo(1);
        // Cơ sở dữ liệu cũng giữ DBR-34: dòng thứ hai cùng khóa mở bị từ chối.
        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO simple_record (kind, module, resident_id, status, content, unique_key, created_at,
                          created_by)
                        SELECT kind, module, resident_id, status, content, unique_key, created_at, created_by
                        FROM simple_record WHERE id = ?
                        """, flagId)).isInstanceOf(DataAccessException.class);
    }
}
