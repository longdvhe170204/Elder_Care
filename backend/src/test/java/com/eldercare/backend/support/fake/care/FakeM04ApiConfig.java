package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Import riêng fake API do M04 công bố, không tự nạp trong production. */
@TestConfiguration
@Profile({"test", "demo"})
public class FakeM04ApiConfig {
    @Bean
    @Primary
    TripMedicationReadApi fakeTripMedicationReadApi(FakeM04ApiFixture fixture) {
        return fixture.api(TripMedicationReadApi.class);
    }

    @Bean
    @Primary
    TripDutyReadApi fakeTripDutyReadApi(FakeM04ApiFixture fixture) {
        return fixture.api(TripDutyReadApi.class);
    }

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

    // 014 T002: bản giả cho 015 (gỡ nhiệm vụ chuyến) và 016 (số liệu hoạt động).
    @Bean
    @Primary
    TripDutyCommandApi fakeTripDutyCommandApi(FakeM04ApiFixture fixture) {
        return fixture.api(TripDutyCommandApi.class);
    }

    @Bean
    @Primary
    ActivityStatisticsApi fakeActivityStatisticsApi(FakeM04ApiFixture fixture) {
        return fixture.api(ActivityStatisticsApi.class);
    }

    @Bean
    @Primary
    PublicCareResultApi fakePublicCareResultApi(FakeM04ApiFixture fixture) {
        return fixture.api(PublicCareResultApi.class);
    }

    // 005 T053 (duyệt 08/11, 09/11/2026): lệnh 007 gọi khi ghi sự cố ngã, lựa chọn chăm sóc giảm nhẹ. Test 007 đặt
    // fixture.reply(CarePlanReviewApi.class, "fallIncidentRecorded", List.of(residentId, incidentId), () -> null);
    // chưa đặt thì lời gọi ném lỗi (kiểm lệnh 007 rollback); không import cấu hình này thì dùng bản thật của 005.
    @Bean
    @Primary
    CarePlanReviewApi fakeCarePlanReviewApi(FakeM04ApiFixture fixture) {
        return fixture.api(CarePlanReviewApi.class);
    }
}
