package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** T001: lớp thật ở bốn tầng hợp lệ, không dựng gói rỗng trái README mới. */
class T001_ModuleSetupTest {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "api.CarePlanApi",
                "internal.status.service.CareResidentLock",
                "scheduling.CareDailyJob",
                "web.NonPerformanceReasonController"
            })
    void T001_featureLayersAreAvailable(String name) {
        assertThatCode(() -> Class.forName("com.eldercare.backend.care." + name))
                .doesNotThrowAnyException();
    }
}
