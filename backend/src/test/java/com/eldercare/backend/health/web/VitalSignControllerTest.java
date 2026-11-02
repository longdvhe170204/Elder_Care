package com.eldercare.backend.health.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.RemeasureVitalSignCommand;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignComponent;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.health.web.request.RecordVitalSignRequest;
import com.eldercare.backend.health.web.request.RemeasureVitalSignRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class VitalSignControllerTest {

    private VitalSignMeasurementService measurementService;
    private CurrentUser currentUser;
    private VitalSignController controller;

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");

    @BeforeEach
    void setUp() {
        measurementService = mock(VitalSignMeasurementService.class);
        currentUser = mock(CurrentUser.class);
        when(currentUser.accountId()).thenReturn(999L);

        controller = new VitalSignController(measurementService, currentUser);
    }

    @Test
    void recordVitalSign_callsServiceAndReturnsCreated() {
        long residentId = 10L;
        long indicatorId = 20L;
        var request = new RecordVitalSignRequest(
                indicatorId,
                null,
                Map.of("VALUE", new BigDecimal("98.50")),
                null,
                NOW,
                null,
                false,
                null,
                "Bình thường",
                null,
                null,
                null);

        var components = Map.of("VALUE", new VitalSignComponent(new BigDecimal("98.50"), VitalSignLevel.NORMAL));
        var savedRecord = new VitalSignRecord(
                100L,
                residentId,
                indicatorId,
                NOW,
                VitalSignLevel.NORMAL,
                VitalSignConfirmationStatus.CONFIRMED,
                components,
                Set.of(),
                NOW);

        when(measurementService.record(any(RecordVitalSignCommand.class), eq(currentUser)))
                .thenReturn(savedRecord);

        var response = controller.recordVitalSign(residentId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(100L);
        assertThat(response.getBody().residentId()).isEqualTo(residentId);
        assertThat(response.getBody().level()).isEqualTo(VitalSignLevel.NORMAL);
        assertThat(response.getBody().values()).containsEntry("VALUE", new BigDecimal("98.50"));
        verify(measurementService).record(any(RecordVitalSignCommand.class), eq(currentUser));
    }

    @Test
    void remeasureVitalSign_callsServiceAndReturnsCreated() {
        long draftId = 100L;
        var request = new RemeasureVitalSignRequest(
                Map.of("VALUE", new BigDecimal("87.00")), null, NOW, null, "Đo lại lần 2", null);

        var components = Map.of("VALUE", new VitalSignComponent(new BigDecimal("87.00"), VitalSignLevel.DANGEROUS));
        var remeasuredRecord = new VitalSignRecord(
                101L,
                10L,
                20L,
                NOW,
                999L,
                null,
                null,
                false,
                1L,
                VitalSignLevel.DANGEROUS,
                VitalSignConfirmationStatus.CONFIRMED,
                null,
                draftId,
                components,
                Set.of(),
                "Đo lại lần 2",
                NOW,
                NOW,
                NOW,
                999L);

        when(measurementService.remeasure(any(RemeasureVitalSignCommand.class), eq(currentUser)))
                .thenReturn(remeasuredRecord);

        var response = controller.remeasureVitalSign(draftId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(101L);
        assertThat(response.getBody().remeasurementFor()).isEqualTo(draftId);
        verify(measurementService).remeasure(any(RemeasureVitalSignCommand.class), eq(currentUser));
    }

    @Test
    void getVitalSigns_returnsFilteredRecords() {
        long residentId = 10L;
        long indicatorId = 20L;
        Instant from = NOW.minusSeconds(3600);
        Instant to = NOW;

        var components = Map.of("VALUE", new VitalSignComponent(new BigDecimal("98.50"), VitalSignLevel.NORMAL));
        var sample = new VitalSignRecord(
                100L,
                residentId,
                indicatorId,
                NOW,
                VitalSignLevel.NORMAL,
                VitalSignConfirmationStatus.CONFIRMED,
                components,
                Set.of(),
                NOW);

        when(measurementService.getHistory(residentId, indicatorId, from, to)).thenReturn(List.of(sample));

        var result = controller.getVitalSigns(residentId, indicatorId, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(100L);
        verify(measurementService).getHistory(residentId, indicatorId, from, to);
    }
}
