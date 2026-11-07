package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.AlertShiftView;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.*;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class FR_031_AlertDeadlineTest {
    private static final Instant NOW = Instant.parse("2030-03-04T03:00:00Z");
    private static final Instant END = NOW.plusSeconds(7200);
    private final AlertStore store = mock(AlertStore.class);
    private final ParameterApi parameters = mock(ParameterApi.class);
    private final AlertStaffPort staff = mock(AlertStaffPort.class);
    private final AtomicReference<AlertRecord> inserted = new AtomicReference<>();
    private final AlertService service = new AlertService(
            store,
            mock(ApplicationEventPublisher.class),
            parameters,
            mock(BusinessTime.class),
            mock(MonitoringLock.class),
            staff,
            null,
            null,
            mock(com.eldercare.backend.incident.internal.alert.service.AlertResponsibility.class));

    @BeforeEach
    void storeInsertedRecord() {
        when(store.insert(any())).thenAnswer(call -> {
            inserted.set(call.getArgument(0));
            return 1L;
        });
        when(store.findById(1L)).thenAnswer(call -> Optional.of(inserted.get()));
    }

    @Test
    void FR_031_mediumReadsConfiguredDurationForEachNewAlert() {
        var value = duration(ParameterCode.CFG_M05_01, Duration.ofMinutes(23));
        when(value.asDuration()).thenReturn(Duration.ofMinutes(23), Duration.ofMinutes(7));
        assertThat(request(IncidentLevel.MEDIUM).acknowledgementDeadline()).isEqualTo(NOW.plusSeconds(1380));
        assertThat(request(IncidentLevel.MEDIUM).acknowledgementDeadline()).isEqualTo(NOW.plusSeconds(420));
        verify(parameters, times(2)).read(ParameterCode.CFG_M05_01);
    }

    @Test
    void FR_031_missingConfigurationDoesNotInventADeadlineOrWriteAnAlert() {
        when(parameters.read(ParameterCode.CFG_M05_01)).thenThrow(new IllegalStateException("Chưa có cấu hình"));
        assertThatThrownBy(() -> request(IncidentLevel.MEDIUM)).isInstanceOf(IllegalStateException.class);
        verify(store, never()).insert(any());
    }

    @Test
    void FR_031_lowUsesCurrentFloorShiftEnd() {
        currentShift(END);
        assertThat(request(IncidentLevel.LOW).acknowledgementDeadline()).isEqualTo(END);
        verify(staff, never()).nextShift(anyLong(), any());
    }

    @Test
    void FR_031_lowAfterHandoverCutoffUsesNextShiftEnd() {
        currentShift(NOW.plusSeconds(600));
        var nextEnd = END.plusSeconds(28800);
        when(staff.nextShift(10L, NOW))
                .thenReturn(Optional.of(new AlertShiftView(2L, 10L, NOW.plusSeconds(600), nextEnd)));
        assertThat(request(IncidentLevel.LOW).acknowledgementDeadline()).isEqualTo(nextEnd);
    }

    @Test
    void FR_031_lowAtHandoverCutoffStillUsesCurrentShiftEnd() {
        currentShift(NOW.plusSeconds(1800));
        assertThat(request(IncidentLevel.LOW).acknowledgementDeadline()).isEqualTo(NOW.plusSeconds(1800));
    }

    @Test
    void FR_031_lowWithoutShiftDoesNotInventAnHourlyDeadline() {
        assertThatThrownBy(() -> request(IncidentLevel.LOW)).isInstanceOf(IllegalStateException.class);
        verify(store, never()).insert(any());
    }

    @Test
    void FR_031_criticalHasNoEscalationDeadline() {
        assertThat(request(IncidentLevel.CRITICAL).acknowledgementDeadline()).isNull();
        verifyNoInteractions(parameters);
    }

    private AlertRecord request(IncidentLevel level) {
        return service.requestAlert(new CreateAlertCommand(
                        100L, 10L, "MANUAL", "MANUAL:1", level, NOW, null, null, "Cảnh báo kiểm thử", 20L))
                .orElseThrow();
    }

    private void currentShift(Instant endsAt) {
        when(staff.shiftAt(10L, NOW))
                .thenReturn(Optional.of(new AlertShiftView(1L, 10L, NOW.minusSeconds(3600), endsAt)));
        duration(ParameterCode.CFG_M09_04, Duration.ofMinutes(30));
    }

    private ParameterValue duration(ParameterCode code, Duration duration) {
        var value = mock(ParameterValue.class);
        when(value.isSet()).thenReturn(true);
        when(value.asDuration()).thenReturn(duration);
        when(parameters.read(code)).thenReturn(value);
        return value;
    }
}
