package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.events.AlertProgressionRequested;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AlertProgressionListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * FR-086: lệnh của module khác (014 FR-047, FR-056) ghi diễn biến vào cảnh báo đang mở theo nguồn, kèm lệnh nguồn, người
 * thực hiện và thời điểm; không tự đóng cảnh báo; giao lại cùng khóa sự kiện không ghi hai lần.
 */
class FR_086_AlertProgressionIT extends AlertRequestTestBase {
    @Autowired
    AlertProgressionListener listener;

    @Test
    void FR_086_progressionIsRecordedOnOpenAlertOfTheSourceOnce() {
        var alert = request("T086:SOURCE", IncidentLevel.MEDIUM, T0, 301);
        var event = new AlertProgressionRequested(
                "M06.VITAL_SIGN_RECORD", 301, "RETURNED", "Đã trở về viện", "K-086-" + alert.id(), 4401L, T0);

        transaction.executeWithoutResult(s -> listener.record(event));
        transaction.executeWithoutResult(s -> listener.record(event));

        var rows = jdbc.queryForList(
                "SELECT actor_id, occurred_at, JSON_UNQUOTE(JSON_EXTRACT(content, '$.content')) text,"
                        + " JSON_UNQUOTE(JSON_EXTRACT(content, '$.sourceType')) source"
                        + " FROM record_event WHERE kind = 'M05.ALERT_PROGRESSION' AND entity_id = ?",
                alert.id());
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("actor_id")).longValue()).isEqualTo(4401L);
        assertThat(rows.get(0).get("text")).isEqualTo("Đã trở về viện");
        assertThat(rows.get(0).get("source")).isEqualTo("M06.VITAL_SIGN_RECORD");
        assertThat(alerts.findById(alert.id()).orElseThrow().status()).isNotEqualTo(AlertStatus.CLOSED);
    }
}
