package com.eldercare.backend.care;

import static org.mockito.Mockito.*;

import com.eldercare.backend.care.internal.scheduling.service.CareDailyWorkRunner;
import com.eldercare.backend.care.scheduling.*;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** NFR-04: công tắc chung phải có tác dụng cả khi không dùng profile test. */
class NFR_04_CareStartupTest {
    @Test
    void NFR_04_globalSwitchDisablesStartupOutsideTestProfile() {
        var environment = new MockEnvironment().withProperty("eldercare.scheduling.enabled", "false");
        assertStartup(environment, false);
    }

    @Test
    void NFR_04_explicitStartupSwitchOverridesEnabledScheduling() {
        var environment = new MockEnvironment()
                .withProperty("eldercare.scheduling.enabled", "true")
                .withProperty("eldercare.jobs.care-minute.on-startup", "false")
                .withProperty("eldercare.jobs.care-daily.on-startup", "false");
        assertStartup(environment, false);
    }

    @Test
    void NFR_04_enabledStartupStillRunsBothJobs() {
        assertStartup(new MockEnvironment(), true);
    }

    private void assertStartup(MockEnvironment environment, boolean expected) {
        var runs = mock(SchedulerRuns.class);
        var time = mock(BusinessTime.class);
        when(time.now()).thenReturn(Instant.parse("2026-10-31T00:00:00Z"));
        when(runs.start(anyString())).thenReturn("startup-test");
        new CareMinuteJob(List.of(), runs, time, environment).atStartup();
        new CareDailyJob(List.of(), mock(CareDailyWorkRunner.class), runs, time, environment).atStartup();
        verify(runs, times(expected ? 2 : 0)).start(anyString());
    }
}
