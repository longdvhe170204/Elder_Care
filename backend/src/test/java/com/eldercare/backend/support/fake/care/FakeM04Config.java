package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
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
    @Primary
    public FakeTripMedicationPreparation fakeTripMedicationPreparation() {
        return new FakeTripMedicationPreparation();
    }

    @Bean
    @Primary
    public FakeTripStaffSchedule fakeTripStaffSchedule() {
        return new FakeTripStaffSchedule();
    }

    @Bean
    @Primary
    public FakeDayCareCalendar fakeDayCareCalendar() {
        return new FakeDayCareCalendar();
    }

    @Bean
    FakeM04Switches fakeM04Switches() {
        return new FakeM04Switches();
    }

    @Bean
    @Primary
    FakeShiftStaff fakeShiftStaff(FakeM04Switches switches) {
        return new FakeShiftStaff(switches);
    }

    @Bean("fakeM04VitalSigns")
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
    public FakeActivityHealthGate fakeActivityHealthGate(FakeM04Switches switches) {
        return new FakeActivityHealthGate(switches);
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

    // 014 T028: QualityCheckPort dùng nguồn thật QualityCheckResultSource của 014, không còn bean giả trong cấu hình.

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

    /** Thay bản thật {@code MealTaskSource} của 011 trong context này: test 005 điều khiển được xác nhận phục vụ. */
    @Bean
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    FakeTaskSource fakeMealTaskSource(FakeM04Switches switches) {
        return new FakeTaskSource(switches, "011", Set.of(SourceType.MEAL));
    }

    /**
     * Bỏ bean thật {@code mealTaskSource} (011, PR #122) khỏi context có bản giả loại {@code MEAL}: {@code CareTaskValidator}
     * đòi đúng một nguồn cho mỗi loại căn cứ, hai nguồn {@code MEAL} làm mọi lệnh lập công việc trả 409
     * {@code INVALID_BASIS}. Chỉ áp cho lớp test import cấu hình này; test của 011 dùng bản thật.
     */
    @Bean
    static org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor removeRealMealTaskSource() {
        return new org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(
                    org.springframework.beans.factory.support.BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition("mealTaskSource")) {
                    registry.removeBeanDefinition("mealTaskSource");
                }
            }

            @Override
            public void postProcessBeanFactory(
                    org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {}
        };
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

    @Bean
    public FakeCareRequirements fakeCareRequirements() {
        return new FakeCareRequirements();
    }

    @Bean
    @Primary
    public FakeCareShifts fakeCareShifts() {
        return new FakeCareShifts();
    }
}
