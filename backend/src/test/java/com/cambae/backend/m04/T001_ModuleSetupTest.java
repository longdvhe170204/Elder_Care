package com.cambae.backend.m04;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** T001: khung gói đã được compiler nhận, không chép đường dẫn logic tiếng Việt. */
class T001_ModuleSetupTest {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "api",
                "careplan",
                "attendance",
                "task",
                "recording",
                "checklist",
                "catalog",
                "rules",
                "overdue",
                "timetable",
                "status",
                "scheduling"
            })
    void T001_featurePackagesAreAvailable(String name) {
        assertThatCode(() -> Class.forName("com.cambae.backend.m04." + name + ".package-info"))
                .doesNotThrowAnyException();
    }
}
