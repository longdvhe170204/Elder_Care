package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.health.api.FacilityContactApi;
import com.eldercare.backend.health.api.MeasurementScheduleApi;
import com.eldercare.backend.health.api.VitalSignStatisticsApi;
import com.eldercare.backend.health.api.dto.FacilityContactView;
import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import com.eldercare.backend.health.api.dto.MeasurementScheduleView;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import com.eldercare.backend.health.api.dto.VitalSignReportView;
import com.eldercare.backend.health.api.events.MeasurementScheduleChanged;
import com.eldercare.backend.health.api.events.MissingPersonalThresholdReminder;
import com.eldercare.backend.health.api.events.VitalSignConfirmed;
import com.eldercare.backend.health.api.events.VitalSignCorrected;
import com.eldercare.backend.support.fake.health.FakeFacilityContact;
import com.eldercare.backend.support.fake.health.FakeHealthConfig;
import com.eldercare.backend.support.fake.health.FakeHealthEvents;
import com.eldercare.backend.support.fake.health.FakeMeasurementSchedules;
import com.eldercare.backend.support.fake.health.FakeVitalSignStatistics;
import com.eldercare.backend.support.fake.health.FakeVitalSigns;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import tools.jackson.databind.json.JsonMapper;

class T003_HealthApiContractTest {
    private static final Instant FROM = Instant.parse("2026-10-31T07:00:00Z");
    private static final Instant TO = FROM.plusSeconds(3600);

    @Test
    void FR_050_rangeSourceSeparatesResidentsIndicatorsAndTimeBoundaries() {
        var first = new VitalSignReadingView(
                201,
                101,
                301,
                Map.of("VALUE", new BigDecimal("60.00")),
                VitalSignLevel.NORMAL,
                VitalSignConfirmationStatus.CONFIRMED,
                FROM,
                Set.of());
        var atEnd = new VitalSignReadingView(
                202,
                101,
                301,
                Map.of("VALUE", new BigDecimal("61.00")),
                VitalSignLevel.WARNING,
                VitalSignConfirmationStatus.CONFIRMED,
                TO,
                Set.of(VitalSignLabel.LATE_RECORDED));
        var fake = new FakeVitalSigns();
        fake.history(101, 301, List.of(first, atEnd));
        fake.vitalSignIndicators(Set.of(301L));

        assertThat(fake.withinRange(101, 301, FROM, TO)).containsExactly(first);
        assertThat(fake.withinRange(102, 301, FROM, TO)).isEmpty();
        assertThat(fake.withinRange(101, 302, FROM, TO)).isEmpty();
        assertThat(fake.vitalSignIndicatorIds()).containsExactly(301L);
    }

    @Test
    void FR_003_scheduleSourceReturnsOnlyExplicitlyConfiguredReadResults() {
        var schedule = new MeasurementScheduleView(
                401,
                101,
                Set.of(301L),
                MeasurementFrequency.CLOCK_TIMES,
                Map.of("times", List.of("14:00")),
                FROM,
                null,
                "MANDATORY",
                "Lịch đo thử");
        var times = new ArrayList<>(List.of(FROM, FROM.plusSeconds(60)));
        var fake = new FakeMeasurementSchedules();
        fake.effective(101, FROM, List.of(schedule));
        fake.occurrences(401, FROM, TO, times);
        times.clear();

        assertThat(fake.effectiveAt(101, FROM)).containsExactly(schedule);
        assertThat(fake.effectiveAt(102, FROM)).isEmpty();
        assertThat(fake.occurrences(401, FROM, TO)).containsExactly(FROM, FROM.plusSeconds(60));
        assertThat(fake.occurrences(402, FROM, TO)).isEmpty();
    }

    @Test
    void FR_070_contactSourceDoesNotInventAFacilityPhoneNumber() {
        var fake = new FakeFacilityContact();
        assertThat(fake.current().phoneNumber()).isNull();

        var configured = new FacilityContactView("0900000001", "Số thử trong test", 3);
        fake.source(configured);

        assertThat(fake.current()).isEqualTo(configured);
    }

    @Test
    void FR_081_statisticsSourceRespectsScopeAndTheRequestedTimeWindow() {
        var floor = new ScopeFilter(false, Set.of(501L), Set.of(), Set.of());
        var first = new VitalSignReportView(201, 101, 301, 501L, FROM, VitalSignLevel.DANGEROUS);
        var atEnd = new VitalSignReportView(202, 101, 301, 501L, TO, VitalSignLevel.NORMAL);
        var fake = new FakeVitalSignStatistics();
        fake.source(floor, List.of(first, atEnd));

        assertThat(fake.readings(floor, FROM, TO).toList()).containsExactly(first);
        assertThat(fake.readings(ScopeFilter.FACILITY, FROM, TO).toList()).isEmpty();
        assertThat(fake.readings(floor, FROM, TO).toList()).containsExactly(first);
    }

    @Test
    void FR_013_eventsKeepOriginalThresholdIdentityAndCanRoundTripThroughTheOutbox() {
        var labels = new HashSet<>(Set.of(VitalSignLabel.NOT_REMEASURED));
        var confirmed = new VitalSignConfirmed(
                201, 101, 301, VitalSignLevel.DANGEROUS, 601L, 701L, FROM, labels, "confirmed:201", TO);
        labels.clear();
        var corrected = new VitalSignCorrected(
                201, 101, 301, VitalSignLevel.DANGEROUS, VitalSignLevel.NORMAL, FROM, "corrected:201:1", TO);
        var changed = new MeasurementScheduleChanged(401, 101, "schedule:401:1", TO);
        var mapper = JsonMapper.builder().build();

        assertThat(confirmed.labels()).containsExactly(VitalSignLabel.NOT_REMEASURED);
        assertThat(confirmed.thresholdVersionId()).isEqualTo(601);
        assertThat(confirmed.taskId()).isEqualTo(701);
        assertThat(mapper.readValue(mapper.writeValueAsString(confirmed), VitalSignConfirmed.class))
                .isEqualTo(confirmed);
        assertThat(mapper.readValue(mapper.writeValueAsString(corrected), VitalSignCorrected.class))
                .isEqualTo(corrected);
        assertThat(mapper.readValue(mapper.writeValueAsString(changed), MeasurementScheduleChanged.class))
                .isEqualTo(changed);
        List<Object> published = new ArrayList<>();
        var events = new FakeHealthEvents(published::add);
        events.confirmed(confirmed);
        events.corrected(corrected);
        events.scheduleChanged(changed);
        assertThat(published).containsExactly(confirmed, corrected, changed);
    }

    @Test
    void FR_025_missingThresholdReminderUsesLowLevelAndTheFacilityDoctorGroup() {
        var event = new MissingPersonalThresholdReminder(101, "threshold:101:episode1", FROM);
        HasNotificationRequests requests = event;
        var request = requests.notificationRequests().getFirst();

        assertThat(request.level()).contains(NotificationLevel.LOW);
        assertThat(request.residentId()).contains(101L);
        assertThat(request.recipientGroups()).containsExactly(new RecipientGroup.FacilityRole(Role.DOCTOR));
        assertThat(request.eventKey()).isEqualTo(event.eventKey());
        assertThat(request.occurredAt()).isEqualTo(FROM);
        var mapper = JsonMapper.builder().build();
        String serialized = mapper.writeValueAsString(event);
        assertThat(serialized.length()).isLessThanOrEqualTo(4000);
        assertThat(mapper.readValue(serialized, MissingPersonalThresholdReminder.class))
                .isEqualTo(event);
    }

    @Test
    void T003_sharedHealthConfigUsesOneFakePerApi() {
        try (var context = new AnnotationConfigApplicationContext(FakeHealthConfig.class)) {
            assertThat(context.getBean(FakeVitalSigns.class)).isNotNull();
            assertThat(context.getBean(MeasurementScheduleApi.class))
                    .isSameAs(context.getBean(FakeMeasurementSchedules.class));
            assertThat(context.getBean(FacilityContactApi.class)).isSameAs(context.getBean(FakeFacilityContact.class));
            assertThat(context.getBean(VitalSignStatisticsApi.class))
                    .isSameAs(context.getBean(FakeVitalSignStatistics.class));
        }
    }
}
