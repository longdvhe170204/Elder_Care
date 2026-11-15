package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.staffing.api.SwapStatisticsApi;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Q-408 (chủ dự án 2026-11-11): bỏ chỉ tiêu số giờ làm trong tháng theo nhân viên ({@code SHIFT.WORKING_HOURS},
 * {@code CA.GIO_LAM}, FR-073) và hàm nguồn của 015.
 */
class Q408_NoWorkingHoursMetricTest {

    @Test
    void Q408_workingHoursMetricAndSourceRemoved() {
        assertThatThrownBy(() ->
                        Class.forName("com.eldercare.backend.reporting.internal.other.service.WorkingHoursMetric"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThat(Arrays.stream(SwapStatisticsApi.class.getMethods()).map(m -> m.getName()))
                .doesNotContain("workingHoursByMonth");
    }
}
