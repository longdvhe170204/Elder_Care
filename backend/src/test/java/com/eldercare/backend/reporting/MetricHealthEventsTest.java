package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import com.eldercare.backend.incident.api.dto.MonitoringView;
import com.eldercare.backend.reporting.internal.incident.service.AlertEscalationsMetric;
import com.eldercare.backend.reporting.internal.incident.service.AlertRecords;
import com.eldercare.backend.reporting.internal.incident.service.AlertResponseTimeMetric;
import com.eldercare.backend.reporting.internal.incident.service.AlertsMetric;
import com.eldercare.backend.reporting.internal.incident.service.CriticalFlagsMetric;
import com.eldercare.backend.reporting.internal.incident.service.FollowUpsMetric;
import com.eldercare.backend.reporting.internal.incident.service.IncidentEscalationsMetric;
import com.eldercare.backend.reporting.internal.incident.service.IncidentRecords;
import com.eldercare.backend.reporting.internal.incident.service.IncidentResponseTimeMetric;
import com.eldercare.backend.reporting.internal.incident.service.IncidentsMetric;
import com.eldercare.backend.reporting.internal.incident.service.NonHealthIncidentsMetric;
import com.eldercare.backend.reporting.internal.incident.service.SosAssistanceAlertsMetric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.resident.service.OverdueReassessmentsMetric;
import com.eldercare.backend.resident.api.ResidentStatisticsApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.support.fake.incident.FakeIncidentStatistics;
import com.eldercare.backend.support.fake.resident.FakeResidentStatistics;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu nhóm sức khỏe, sự cố (016 T017, T018, T031, T032; tên logic {@code ChiTieu_SK_*Test}; FR-020, FR-041,
 * FR-042, FR-042a, FR-044; quickstart Q20, Q21) đọc {@code IncidentStatisticsApi} của 007 và
 * {@code ResidentStatisticsApi} của 001: trạng thái, mức, số lần leo thang, mốc tiếp nhận do nguồn xác định (FR-002).
 */
class MetricHealthEventsTest {

    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-08T00:00:00Z");
    private static final ScopeFilter ALL = ScopeFilter.FACILITY;

    private final FakeIncidentStatistics incidents = new FakeIncidentStatistics();
    private final FakeResidentStatistics residents = new FakeResidentStatistics();
    private final ObjectProvider<IncidentStatisticsApi> incidentApi =
            new StaticListableBeanFactory(Map.of("incidents", incidents)).getBeanProvider(IncidentStatisticsApi.class);
    private final ObjectProvider<ResidentLocationApi> noLocations =
            new StaticListableBeanFactory().getBeanProvider(ResidentLocationApi.class);
    private final AlertRecords alerts = new AlertRecords(incidentApi);
    private final IncidentRecords incidentRecords = new IncidentRecords(incidentApi);

    private static Instant at(int minutes) {
        return FROM.plus(Duration.ofMinutes(minutes));
    }

    private static AlertReportView alert(
            long id, String level, String levelAtAck, int createdMin, Integer ackMin, int escalations, String source) {
        return new AlertReportView(
                id,
                7L,
                level,
                levelAtAck,
                at(createdMin),
                ackMin == null ? null : at(ackMin),
                escalations,
                false,
                "VITAL_SIGN",
                101L,
                source,
                null,
                false);
    }

    private static IncidentReportView incident(
            long id, String level, String type, boolean nonHealth, int createdMin, Integer ackMin, int escalations) {
        return new IncidentReportView(
                id,
                8L,
                level,
                ackMin == null ? null : level,
                type,
                nonHealth,
                102L,
                at(createdMin),
                ackMin == null ? null : at(ackMin),
                escalations,
                ackMin == null ? "NEW" : "IN_PROGRESS");
    }

    private static <R extends MetricRecord> MetricValue value(MetricSource<R> source) {
        return source.metric().compute(included(source));
    }

    private static <R extends MetricRecord> List<R> included(MetricSource<R> source) {
        return source.metric().included(source.records(ALL, FROM, TO, ReportFilters.NONE), r -> true);
    }

    private static List<Tuple2> breakdown(MetricValue v) {
        return v.breakdown().stream()
                .map(b -> new Tuple2(b.key(), b.value().stripTrailingZeros().toPlainString()))
                .toList();
    }

    private record Tuple2(String key, String value) {}

    @Test
    void FR_041_alerts_countByCurrentLevel_responseTimeOnlyAcknowledged_byLevelAtAcknowledgement() {
        incidents.alerts(
                ALL,
                List.of(
                        alert(1, "DANGER", "WARNING", 0, 10, 1, "OTHER"),
                        alert(2, "WARNING", "WARNING", 0, 20, 0, "OTHER"),
                        alert(3, "DANGER", null, 0, null, 2, "OTHER"),
                        alert(4, "WARNING", "WARNING", 7 * 24 * 60, 7 * 24 * 60 + 5, 0, "OTHER")));

        AlertsMetric count = new AlertsMetric(alerts);
        MetricValue total = value(count);
        assertThat(count.metric().code()).isEqualTo("HEALTH.ALERTS");
        assertThat(count.metric().group()).isEqualTo(MetricGroup.HEALTH_EVENTS);
        assertThat(total.value()).isEqualByComparingTo("3");
        assertThat(breakdown(total)).containsExactly(new Tuple2("DANGER", "2"), new Tuple2("WARNING", "1"));

        // chỉ cảnh báo đã tiếp nhận; mức lúc tiếp nhận; cảnh báo 3 chưa tiếp nhận không vào trung bình
        AlertResponseTimeMetric response = new AlertResponseTimeMetric(alerts);
        MetricValue minutes = value(response);
        assertThat(response.metric().code()).isEqualTo("HEALTH.ALERT_RESPONSE_TIME");
        assertThat(minutes.value()).isEqualByComparingTo("15");
        assertThat(breakdown(minutes)).containsExactly(new Tuple2("WARNING", "15"));
        assertThat(response.metric().details(included(response), null))
                .extracting(r -> r.sourceKey())
                .containsExactly("M05.ALERT:1", "M05.ALERT:2");

        AlertEscalationsMetric escalations = new AlertEscalationsMetric(alerts);
        assertThat(escalations.metric().code()).isEqualTo("HEALTH.ALERT_ESCALATIONS");
        assertThat(value(escalations).value()).isEqualByComparingTo("3");
    }

    @Test
    void FR_041_Q374_sosAndAssistButton_ownRowsBySource_detailCarriesArrival() {
        incidents.alerts(
                ALL,
                List.of(
                        alert(1, "DANGER", "DANGER", 0, 2, 0, "SOS"),
                        new AlertReportView(
                                2,
                                null,
                                "DANGER",
                                "DANGER",
                                at(0),
                                at(3),
                                0,
                                false,
                                "HELP",
                                103L,
                                "EMERGENCY_ASSIST",
                                at(9),
                                false),
                        new AlertReportView(
                                3,
                                null,
                                "DANGER",
                                "DANGER",
                                at(0),
                                at(4),
                                0,
                                false,
                                "HELP",
                                null,
                                "EMERGENCY_ASSIST",
                                null,
                                true),
                        alert(4, "WARNING", "WARNING", 0, 1, 0, "OTHER")));

        SosAssistanceAlertsMetric sos = new SosAssistanceAlertsMetric(alerts);
        MetricValue v = value(sos);

        assertThat(sos.metric().code()).isEqualTo("HEALTH.SOS_ASSISTANCE_ALERTS");
        assertThat(v.value()).isEqualByComparingTo("3");
        assertThat(breakdown(v)).containsExactly(new Tuple2("EMERGENCY_ASSIST", "2"), new Tuple2("SOS", "1"));
        assertThat(sos.metric().details(included(sos), "EMERGENCY_ASSIST"))
                .extracting(
                        r -> r.result().get("responseMinutes"),
                        r -> r.result().get("arrivalMinutes"),
                        r -> r.result().get("arrivalTimeMissing"))
                .containsExactly(tuple(3L, 9L, false), tuple(4L, null, true));
        // cảnh báo SOS vẫn trong tổng theo mức
        assertThat(value(new AlertsMetric(alerts)).value()).isEqualByComparingTo("4");
    }

    @Test
    void FR_042_incidents_byCurrentLevel_escalations_responseTimeOnlyAcknowledged() {
        incidents.incidents(
                ALL,
                List.of(
                        incident(1, "SERIOUS", "FALL", false, 0, 30, 1),
                        incident(2, "MINOR", "FALL", false, 0, 10, 0),
                        incident(3, "SERIOUS", "LOST_BELONGING", true, 0, null, 2)));

        IncidentsMetric count = new IncidentsMetric(incidentRecords);
        MetricValue v = value(count);
        assertThat(count.metric().code()).isEqualTo("HEALTH.INCIDENTS");
        assertThat(count.metric().group()).isEqualTo(MetricGroup.HEALTH_EVENTS);
        assertThat(v.value()).isEqualByComparingTo("3");
        assertThat(breakdown(v)).containsExactly(new Tuple2("MINOR", "1"), new Tuple2("SERIOUS", "2"));

        IncidentResponseTimeMetric response = new IncidentResponseTimeMetric(incidentRecords);
        assertThat(response.metric().code()).isEqualTo("HEALTH.INCIDENT_RESPONSE_TIME");
        assertThat(value(response).value()).isEqualByComparingTo("20");

        IncidentEscalationsMetric escalations = new IncidentEscalationsMetric(incidentRecords);
        assertThat(escalations.metric().code()).isEqualTo("HEALTH.INCIDENT_ESCALATIONS");
        assertThat(value(escalations).value()).isEqualByComparingTo("3");
    }

    @Test
    void FR_042a_nonHealthIncidents_onlyFlaggedTypes_rowWithoutResult() {
        incidents.incidents(
                ALL,
                List.of(
                        incident(1, "SERIOUS", "FALL", false, 0, 30, 1),
                        incident(2, "MINOR", "LOST_BELONGING", true, 0, null, 0),
                        incident(3, "MINOR", "FACILITY", true, 5, 9, 0)));

        NonHealthIncidentsMetric nonHealth = new NonHealthIncidentsMetric(incidentRecords);
        MetricValue v = value(nonHealth);

        assertThat(nonHealth.metric().code()).isEqualTo("HEALTH.NON_HEALTH_INCIDENTS");
        assertThat(nonHealth.metric().group()).isEqualTo(MetricGroup.NON_HEALTH_INCIDENTS);
        assertThat(v.value()).isEqualByComparingTo("2");
        assertThat(breakdown(v)).containsExactly(new Tuple2("FACILITY", "1"), new Tuple2("LOST_BELONGING", "1"));
        assertThat(nonHealth.metric().details(included(nonHealth), null))
                .allSatisfy(row -> {
                    assertThat(row.result()).isEmpty();
                    assertThat(row.resultGroup()).isNull();
                    assertThat(row.residentId()).isEqualTo(8L);
                })
                .extracting(r -> r.type(), r -> r.status())
                .containsExactly(tuple("LOST_BELONGING", "MINOR"), tuple("FACILITY", "MINOR"));
    }

    @Test
    void FR_042_FR_020_followUps_criticalFlags_overdueReassessments_currentValues() {
        incidents.monitoring(
                ALL,
                List.of(
                        new MonitoringView(7, "POST_FALL", 101L, at(0), at(72 * 60)),
                        new MonitoringView(8, "CONTACT", 102L, at(0), at(14 * 24 * 60)),
                        new MonitoringView(9, "CONTACT", 102L, at(0), at(14 * 24 * 60))));
        incidents.critical(ALL, List.of(7L, 9L));
        residents.resident(7, 101L, 1L).overdue(7, at(0));

        FollowUpsMetric followUps = new FollowUpsMetric(incidentApi);
        MetricValue f = value(followUps);
        assertThat(followUps.metric().code()).isEqualTo("HEALTH.FOLLOW_UPS");
        assertThat(f.value()).isEqualByComparingTo("3");
        assertThat(breakdown(f)).containsExactly(new Tuple2("CONTACT", "2"), new Tuple2("POST_FALL", "1"));

        CriticalFlagsMetric critical = new CriticalFlagsMetric(incidentApi, noLocations);
        assertThat(critical.metric().code()).isEqualTo("HEALTH.CRITICAL_FLAGS");
        assertThat(critical.metric().group()).isEqualTo(MetricGroup.CRITICAL_FLAGS);
        assertThat(value(critical).value()).isEqualByComparingTo("2");

        OverdueReassessmentsMetric overdue = new OverdueReassessmentsMetric(
                new StaticListableBeanFactory(Map.of("residents", residents))
                        .getBeanProvider(ResidentStatisticsApi.class),
                noLocations);
        assertThat(overdue.metric().code()).isEqualTo("HEALTH.OVERDUE_REASSESSMENTS");
        assertThat(overdue.metric().group()).isEqualTo(MetricGroup.OVERDUE_REASSESSMENTS);
        assertThat(value(overdue).value()).isEqualByComparingTo("1");
        assertThat(overdue.metric().details(included(overdue), null))
                .singleElement()
                .satisfies(r -> assertThat(r.residentId()).isEqualTo(7L));
    }
}
