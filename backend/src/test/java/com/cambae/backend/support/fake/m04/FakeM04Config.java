package com.cambae.backend.support.fake.m04;

import com.cambae.backend.m04.api.spi.TaskSourcePort.SourceType;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** T002: opt-in bằng @Import; không có trong JAR production, không thay nguồn thật ngầm định.
 * Các SPI nhiều nguồn không đánh @Primary: consumer nhận List, phải chọn đúng loại nguồn.
 */
@TestConfiguration
@Profile({"test", "demo"})
public class FakeM04Config {
    @Bean
    FakeM04Switches fakeM04Switches() {
        return new FakeM04Switches();
    }

    @Bean
    @Primary
    FakeShiftStaff fakeShiftStaff(FakeM04Switches switches) {
        return new FakeShiftStaff(switches);
    }

    @Bean
    @Primary
    FakeVitalSigns fakeVitalSigns(FakeM04Switches switches) {
        return new FakeVitalSigns(switches);
    }

    @Bean
    @Primary
    FakeMedicationChecklist fakeMedicationChecklist(FakeM04Switches switches) {
        return new FakeMedicationChecklist(switches);
    }

    @Bean
    @Primary
    FakeAlertReader fakeAlertReader(FakeM04Switches switches) {
        return new FakeAlertReader(switches);
    }

    @Bean
    @Primary
    FakeDayCarePickups fakeDayCarePickups(FakeM04Switches switches) {
        return new FakeDayCarePickups(switches);
    }

    @Bean
    @Primary
    FakeQualityChecks fakeQualityChecks(FakeM04Switches switches) {
        return new FakeQualityChecks(switches);
    }

    @Bean
    FakeTaskSource fakeHealthTaskSource(FakeM04Switches switches) {
        return new FakeTaskSource(
                switches,
                "007",
                Set.of(
                        SourceType.MEASUREMENT,
                        SourceType.POST_FALL_MONITORING,
                        SourceType.CONTACT_MONITORING,
                        SourceType.WATER_INTAKE_ALERT));
    }

    @Bean
    FakeTaskSource fakeMealTaskSource(FakeM04Switches switches) {
        return new FakeTaskSource(switches, "011", Set.of(SourceType.MEAL));
    }

    @Bean
    FakeTaskSource fakeActivityTaskSource(FakeM04Switches switches) {
        return new FakeTaskSource(switches, "014", Set.of(SourceType.ACTIVITY_SESSION, SourceType.REWORK));
    }

    @Bean
    FakeTimetableSource fakeMedicationTimetable(FakeM04Switches switches) {
        return new FakeTimetableSource(switches, "006");
    }

    @Bean
    FakeTimetableSource fakePickupTimetable(FakeM04Switches switches) {
        return new FakeTimetableSource(switches, "012");
    }

    @Bean
    FakeTimetableSource fakeActivityTimetable(FakeM04Switches switches) {
        return new FakeTimetableSource(switches, "014");
    }

    @Bean
    FakeM04Controller fakeM04Controller(FakeM04Switches switches) {
        return new FakeM04Controller(switches);
    }
}
