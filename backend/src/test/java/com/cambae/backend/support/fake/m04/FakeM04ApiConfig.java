package com.cambae.backend.support.fake.m04;

import com.cambae.backend.m04.api.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Import riêng fake API do M04 công bố, không tự nạp trong production. */
@TestConfiguration
@Profile({"test", "demo"})
public class FakeM04ApiConfig {
    @Bean
    FakeM04ApiFixture fakeM04ApiFixture() {
        return new FakeM04ApiFixture();
    }

    @Bean
    @Primary
    CarePlanApi fakeCarePlanApi(FakeM04ApiFixture fixture) {
        return fixture.api(CarePlanApi.class);
    }

    @Bean
    @Primary
    TaskTypeApi fakeTaskTypeApi(FakeM04ApiFixture fixture) {
        return fixture.api(TaskTypeApi.class);
    }

    @Bean
    @Primary
    DayCareAttendanceApi fakeDayCareAttendanceApi(FakeM04ApiFixture fixture) {
        return fixture.api(DayCareAttendanceApi.class);
    }

    @Bean
    @Primary
    ShiftTaskApi fakeShiftTaskApi(FakeM04ApiFixture fixture) {
        return fixture.api(ShiftTaskApi.class);
    }

    @Bean
    @Primary
    HandoverTaskApi fakeHandoverTaskApi(FakeM04ApiFixture fixture) {
        return fixture.api(HandoverTaskApi.class);
    }

    @Bean
    @Primary
    TaskSourceApi fakeTaskSourceApi(FakeM04ApiFixture fixture) {
        return fixture.api(TaskSourceApi.class);
    }

    @Bean
    @Primary
    AbsenceTaskApi fakeAbsenceTaskApi(FakeM04ApiFixture fixture) {
        return fixture.api(AbsenceTaskApi.class);
    }

    @Bean
    @Primary
    VitalSignTaskApi fakeVitalSignTaskApi(FakeM04ApiFixture fixture) {
        return fixture.api(VitalSignTaskApi.class);
    }

    @Bean
    @Primary
    TaskStatisticsApi fakeTaskStatisticsApi(FakeM04ApiFixture fixture) {
        return fixture.api(TaskStatisticsApi.class);
    }

    @Bean
    @Primary
    TimetableApi fakeTimetableApi(FakeM04ApiFixture fixture) {
        return fixture.api(TimetableApi.class);
    }

    @Bean
    @Primary
    PublicCareResultApi fakePublicCareResultApi(FakeM04ApiFixture fixture) {
        return fixture.api(PublicCareResultApi.class);
    }
}
