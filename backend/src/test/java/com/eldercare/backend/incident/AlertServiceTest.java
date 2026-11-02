package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.AlertCreated;
import com.eldercare.backend.incident.api.events.AlertLevelRaised;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AlertServiceTest {

    private AlertStore alertStore;
    private ApplicationEventPublisher eventPublisher;
    private ParameterApi parameterApi;
    private BusinessTime businessTime;
    private MonitoringLock monitoringLock;
    private AlertStaffPort staffPort;
    private ResidentApi residentApi;
    private RecordEventApi recordEvents;
    private AlertService alertService;

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    @BeforeEach
    void setUp() {
        alertStore = mock(AlertStore.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        parameterApi = mock(ParameterApi.class);
        businessTime = mock(BusinessTime.class);
        monitoringLock = mock(MonitoringLock.class);
        staffPort = mock(AlertStaffPort.class);
        residentApi = mock(ResidentApi.class);
        recordEvents = mock(RecordEventApi.class);

        when(businessTime.now()).thenReturn(NOW);
        var deadline = mock(com.eldercare.backend.administration.parameter.api.dto.ParameterValue.class);
        when(deadline.asDuration()).thenReturn(java.time.Duration.ofMinutes(15));
        when(parameterApi.read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M05_01))
                .thenReturn(deadline);

        alertService = new AlertService(
                alertStore,
                eventPublisher,
                parameterApi,
                businessTime,
                monitoringLock,
                staffPort,
                residentApi,
                recordEvents);
    }

    @Test
    void requestAlert_skipsCreationWhenResidentIsInFinalStatus() {
        long residentId = 100L;
        when(residentApi.isFinalStatus(residentId)).thenReturn(true);

        var cmd = new CreateAlertCommand(
                residentId,
                null,
                "CHI_SO",
                "VUOT_NGUONG:1",
                IncidentLevel.MEDIUM,
                NOW,
                "CHI_SO",
                200L,
                "SpO2 cảnh báo",
                null);

        var result = alertService.requestAlert(cmd);

        assertThat(result).isEmpty();
        verify(alertStore, never()).insert(any());
        verify(monitoringLock, never()).lock(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void requestAlert_createsNewAlertAndLocksResident() {
        long residentId = 100L;
        when(residentApi.isFinalStatus(residentId)).thenReturn(false);
        when(alertStore.findOpenAlert(residentId, "VUOT_NGUONG:1")).thenReturn(Optional.empty());
        when(alertStore.insert(any())).thenReturn(500L);

        var sampleRecord = new AlertRecord(
                500L,
                residentId,
                null,
                "CHI_SO",
                "VUOT_NGUONG:1",
                IncidentLevel.MEDIUM,
                null,
                1,
                NOW,
                NOW,
                AlertStatus.NEW,
                0,
                NOW.plusSeconds(900),
                null,
                0,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                null,
                "SpO2 cảnh báo",
                null,
                null,
                null,
                false,
                0);
        when(alertStore.findById(500L)).thenReturn(Optional.of(sampleRecord));

        var cmd = new CreateAlertCommand(
                residentId,
                null,
                "CHI_SO",
                "VUOT_NGUONG:1",
                IncidentLevel.MEDIUM,
                NOW,
                "CHI_SO",
                200L,
                "SpO2 cảnh báo",
                null);

        var result = alertService.requestAlert(cmd);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(500L);
        verify(monitoringLock).lock(residentId);
        verify(recordEvents).add(any(), any(), any());
        verify(eventPublisher).publishEvent(any(AlertCreated.class));
    }

    @Test
    void requestAlert_mergesOpenAlertAndRaisesLevel() {
        long residentId = 100L;
        when(residentApi.isFinalStatus(residentId)).thenReturn(false);

        var openRecord = new AlertRecord(
                500L,
                residentId,
                null,
                "CHI_SO",
                "VUOT_NGUONG:1",
                IncidentLevel.MEDIUM,
                null,
                1,
                NOW.minusSeconds(300),
                NOW.minusSeconds(300),
                AlertStatus.NEW,
                0,
                NOW.plusSeconds(600),
                null,
                0,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                null,
                "SpO2 cảnh báo",
                null,
                null,
                null,
                false,
                1);
        when(alertStore.findOpenAlert(residentId, "VUOT_NGUONG:1")).thenReturn(Optional.of(openRecord));

        var cmd = new CreateAlertCommand(
                residentId,
                null,
                "CHI_SO",
                "VUOT_NGUONG:1",
                IncidentLevel.CRITICAL,
                NOW,
                "CHI_SO",
                201L,
                "SpO2 nguy hiểm",
                null);

        var result = alertService.requestAlert(cmd);

        assertThat(result).isPresent();
        assertThat(result.get().occurrenceCount()).isEqualTo(2);
        assertThat(result.get().level()).isEqualTo(IncidentLevel.CRITICAL);
        verify(alertStore).updateMerged(eq(500L), eq(2), eq(NOW), eq(IncidentLevel.CRITICAL), isNull(), eq(1));
        verify(eventPublisher).publishEvent(any(AlertLevelRaised.class));
    }
}
