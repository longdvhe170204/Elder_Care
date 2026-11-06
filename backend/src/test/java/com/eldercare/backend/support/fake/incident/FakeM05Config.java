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
    public FakeIncidents fakeIncidents(FakeAlerts alerts) {
        return new FakeIncidents(alerts);
    }

    @Bean
    @Primary
    public FakeAlerts fakeAlerts() {
        return new FakeAlerts();
    }

    @Bean
    @Primary
    public FakeIsolation fakeIsolation() {
        return new FakeIsolation();
    }

    @Bean
    @Primary
    public FakeFamilyPortalHealth fakeFamilyPortalHealth() {
        return new FakeFamilyPortalHealth();
    }

    @Bean
    @Primary
    public FakeIncidentStatistics fakeIncidentStatistics() {
        return new FakeIncidentStatistics();
    }

    @Bean
    @Primary
    public FakeIncidentCommands fakeIncidentCommands() {
        return new FakeIncidentCommands();
    }

    @Bean
    @Primary
    public FakeIncidentTypes fakeIncidentTypes() {
        return new FakeIncidentTypes();
    }

    @Bean
    @Primary
    public com.eldercare.backend.incident.api.AlertCommandApi fakeAlertCommands(FakeIncidentCommands commands) {
        return commands::acknowledgeAlert;
    }

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
