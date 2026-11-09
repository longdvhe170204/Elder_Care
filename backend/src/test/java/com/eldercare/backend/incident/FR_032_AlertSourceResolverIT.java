package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AlertSourceResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * FR-032, research S-09: yêu cầu đóng theo nguồn đánh dấu nguồn đã xử lý; mọi nguồn đã xử lý thì cảnh báo đang mở Đã
 * đóng, người đóng Hệ thống; giao lại sự kiện không đổi kết quả.
 */
class FR_032_AlertSourceResolverIT extends AlertRequestTestBase {
    private static final String TYPE = "M06.VITAL_SIGN_RECORD";

    @Autowired
    AlertSourceResolver resolver;

    @Test
    void FR_032_alertClosesOnlyWhenEverySourceIsResolvedAndRedeliveryIsHarmless() {
        var first = request("T017:TASK", IncidentLevel.MEDIUM, T0, 101);
        request("T017:TASK", IncidentLevel.MEDIUM, T0.plusSeconds(60), 102);

        transaction.executeWithoutResult(s -> resolver.resolve(TYPE, 101, "PERFORMED_IN_WINDOW", T0));
        assertThat(alerts.findById(first.id()).orElseThrow().status()).isNotEqualTo(AlertStatus.CLOSED);

        transaction.executeWithoutResult(s -> resolver.resolve(TYPE, 101, "PERFORMED_IN_WINDOW", T0));
        assertThat(alerts.findById(first.id()).orElseThrow().status()).isNotEqualTo(AlertStatus.CLOSED);

        transaction.executeWithoutResult(s -> resolver.resolve(TYPE, 102, "PERFORMED_IN_WINDOW", T0));
        var stored = alerts.findById(first.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(AlertStatus.CLOSED);
        assertThat(stored.closedBySystem()).isTrue();
        assertThat(stored.resolution()).contains("PERFORMED_IN_WINDOW");

        transaction.executeWithoutResult(s -> resolver.resolve(TYPE, 999, "x", T0));
    }
}
