package com.eldercare.backend.support.fake.incident;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Bản giả dùng chung, chỉ được nạp khi test import. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeM05Config {
    @Bean
    @Primary
    public FakeEmergencyFamily fakeEmergencyFamily() {
        return new FakeEmergencyFamily();
    }

    @Bean
    @Primary
    public FakeAttendanceTrace fakeAttendanceTrace() {
        return new FakeAttendanceTrace();
    }

    @Bean
    @Primary
    public FakeNotificationLog fakeNotificationLog() {
        return new FakeNotificationLog();
    }

    @Bean
    @Primary
    public FakeAlertStaff fakeAlertStaff() {
        return new FakeAlertStaff();
    }

    @Bean
    public FakeIncidentEvents fakeIncidentEvents(ApplicationEventPublisher publisher) {
        return new FakeIncidentEvents(publisher);
    }
}
