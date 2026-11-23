package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.service.AlertSourceResolver;
import org.junit.jupiter.api.Test;

/** Q-442: hệ thống chỉ tự đóng cảnh báo quá hạn của công việc; cảnh báo tình trạng sức khỏe do người đóng. */
class Q_442_SystemCloseScopeTest {
    @Test
    void overdueTaskAndMissedMeasurementCloseBySystem() {
        assertThat(AlertSourceResolver.closesBySystem(TypeLock.forOverdueTask(44)))
                .isTrue();
        assertThat(AlertSourceResolver.closesBySystem(TypeLock.forMissedMeasurement(5)))
                .isTrue();
    }

    @Test
    void healthConditionAlertsStayOpenForPeople() {
        assertThat(AlertSourceResolver.closesBySystem(TypeLock.forVitalSign(5))).isFalse();
        assertThat(AlertSourceResolver.closesBySystem(TypeLock.forTrend("UONG_NUOC_THIEU")))
                .isFalse();
        assertThat(AlertSourceResolver.closesBySystem(TypeLock.forManual(3))).isFalse();
        assertThat(AlertSourceResolver.closesBySystem(null)).isFalse();
    }
}
