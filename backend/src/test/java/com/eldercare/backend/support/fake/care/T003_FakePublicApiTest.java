package com.eldercare.backend.support.fake.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class T003_FakePublicApiTest {
    @Test
    void T003_configRegistersEveryPublishedApiAndFixturesRespectAllArguments() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=test")
                .withUserConfiguration(FakeM04ApiConfig.class)
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(CarePlanApi.class)
                            .hasSingleBean(TaskTypeApi.class)
                            .hasSingleBean(DayCareAttendanceApi.class)
                            .hasSingleBean(ShiftTaskApi.class)
                            .hasSingleBean(HandoverTaskApi.class)
                            .hasSingleBean(TaskSourceApi.class)
                            .hasSingleBean(AbsenceTaskApi.class)
                            .hasSingleBean(VitalSignTaskApi.class)
                            .hasSingleBean(TaskStatisticsApi.class)
                            .hasSingleBean(TimetableApi.class)
                            .hasSingleBean(PublicCareResultApi.class);
                    var fixture = context.getBean(FakeM04ApiFixture.class);
                    var api = context.getBean(CarePlanApi.class);
                    var day = LocalDate.of(2026, 10, 27);
                    fixture.reply(CarePlanApi.class, "waterTargetMl", List.of(1L, day), () -> Optional.of(1234));
                    assertThat(api.waterTargetMl(1, day)).contains(1234);
                    assertThatThrownBy(() -> api.waterTargetMl(2, day)).isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(() -> context.getBean(ShiftTaskApi.class).onAbsence(1, 2))
                            .isInstanceOf(IllegalStateException.class);
                    fixture.reset();
                    assertThatThrownBy(() -> api.waterTargetMl(1, day)).isInstanceOf(IllegalStateException.class);
                });
    }
}
