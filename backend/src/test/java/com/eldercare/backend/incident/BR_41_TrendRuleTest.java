package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.events.MeasurementTaskMissed;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleView;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.trend.service.TrendRuleService;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * BR-M05-04, FR-039, FR-039b: bỏ lỡ lần đo theo lịch tạo cảnh báo theo từng chỉ số (khóa {@code BO_LO_LAN_DO:<chiSo>}),
 * việc Bắt buộc tối thiểu Trung bình, giao lại không gộp hai lần; từ chối cùng một thuốc theo mức của quy tắc, quy tắc tắt
 * thì không tạo.
 */
class BR_41_TrendRuleTest {
    private static final Instant AT = Instant.parse("2026-11-08T02:00:00Z");

    private final IncidentCatalogService catalog = mock(IncidentCatalogService.class);
    private final AlertService alerts = mock(AlertService.class);
    private final ReminderMarkers markers = mock(ReminderMarkers.class);
    private final TrendRuleService trends = new TrendRuleService(
            catalog,
            mock(ParameterApi.class),
            mock(VitalSignApi.class),
            alerts,
            markers,
            mock(ApplicationEventPublisher.class),
            mock(RecordEventApi.class),
            mock(ResidencyDayApi.class));

    private void rules(boolean missed, boolean refusal) {
        when(catalog.trendRules())
                .thenReturn(List.of(
                        new TrendRuleView("MISSED_MEASUREMENT", "Bỏ lỡ", missed, IncidentLevel.LOW, null, 1),
                        new TrendRuleView(
                                "MEDICATION_REFUSAL", "Từ chối", refusal, IncidentLevel.MEDIUM, "CFG-M05-04", 1)));
    }

    @Test
    void FR_039b_missedMandatoryMeasurementAlertsPerIndicatorAtLeastMediumOnce() {
        rules(true, true);
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true, true, false, false);
        var event = new MeasurementTaskMissed(
                9,
                101,
                5,
                Set.of(3L, 4L),
                "NOT_DONE",
                Importance.MANDATORY,
                new CareEventContext("M04.TASK", 9, "MISSED", AT, Optional.of(7L), List.of()));

        trends.onMeasurementTaskMissed(event);
        trends.onMeasurementTaskMissed(event);

        var commands = ArgumentCaptor.forClass(CreateAlertCommand.class);
        verify(alerts, times(2)).requestAlert(commands.capture());
        assertThat(commands.getAllValues())
                .extracting(CreateAlertCommand::typeLock)
                .containsExactlyInAnyOrder(TypeLock.forMissedMeasurement(3), TypeLock.forMissedMeasurement(4));
        assertThat(commands.getAllValues()).allMatch(c -> c.level() == IncidentLevel.MEDIUM);
    }

    @Test
    void FR_039_refusalUsesRuleLevelAndDisabledRuleCreatesNothing() {
        rules(true, true);
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);
        var refusal = new MedicationAlertRequestedEvent(
                "TU_CHOI_LIEN_TIEP", "NHE", 101, "dose:77", "refusal:101:12", AT, List.of());

        trends.onMedicationAlertRequested(refusal);

        var command = ArgumentCaptor.forClass(CreateAlertCommand.class);
        verify(alerts).requestAlert(command.capture());
        assertThat(command.getValue().level()).isEqualTo(IncidentLevel.MEDIUM);
        assertThat(command.getValue().typeLock()).isEqualTo(TypeLock.forMedication("TU_CHOI_LIEN_TIEP", 12));

        rules(false, false);
        trends.onMedicationAlertRequested(refusal);
        trends.onMeasurementTaskMissed(new MeasurementTaskMissed(
                9,
                101,
                5,
                Set.of(3L),
                "NOT_DONE",
                Importance.MANDATORY,
                new CareEventContext("M04.TASK", 9, "MISSED", AT, Optional.of(7L), List.of())));
        verify(alerts, times(1)).requestAlert(any(CreateAlertCommand.class));
    }
}
