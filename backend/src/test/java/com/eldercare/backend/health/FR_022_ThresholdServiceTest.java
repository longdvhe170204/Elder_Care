package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.health.internal.threshold.domain.NewThreshold;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdIndicator;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdVersion;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.health.internal.threshold.service.MissingThresholdReminders;
import com.eldercare.backend.health.internal.threshold.service.ThresholdIndicators;
import com.eldercare.backend.health.internal.threshold.service.ThresholdService;
import com.eldercare.backend.resident.api.ResidentApi;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/** Vòng đời phiên bản ngưỡng (bảng FR-022, FR-020, FR-021, FR-023; research S-07). */
class FR_022_ThresholdServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-04T06:00:30Z");
    private static final long TEMPERATURE = 3L;

    private final ThresholdVersionStore store = mock(ThresholdVersionStore.class);
    private final ThresholdIndicators indicators = mock(ThresholdIndicators.class);
    private final ResidentApi residents = mock(ResidentApi.class);
    private final VitalSignLock residentLock = mock(VitalSignLock.class);
    private final TechnicalLocks locks = mock(TechnicalLocks.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final MissingThresholdReminders reminders = mock(MissingThresholdReminders.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final ThresholdService service =
            new ThresholdService(store, indicators, residents, residentLock, locks, audit, time, reminders);
    private final Map<String, ThresholdBand> valid = Map.of(
            "VALUE",
            new ThresholdBand(
                    new BigDecimal("35"), new BigDecimal("36"), new BigDecimal("37.5"), new BigDecimal("39")));

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(indicators.require(TEMPERATURE))
                .thenReturn(new ThresholdIndicator(
                        TEMPERATURE,
                        "TEMPERATURE",
                        Set.of("VALUE"),
                        JsonMapper.builder().build().readTree("{\"VALUE\":{\"min\":30,\"max\":45}}")));
        when(store.open("FACILITY", null, TEMPERATURE)).thenReturn(List.of());
        when(store.open("PERSONAL", 101L, TEMPERATURE)).thenReturn(List.of());
        when(store.insert(any(), any(), anyLong(), any())).thenReturn(50L);
    }

    private static ThresholdVersion version(long id, String scope, Long resident, String status, Instant from) {
        return new ThresholdVersion(id, scope, resident, TEMPERATURE, from, null, status, Map.of(), 2);
    }

    private static NewThreshold facility(Instant from, Map<String, ThresholdBand> bands, Reason reason) {
        return new NewThreshold("FACILITY", null, TEMPERATURE, from, bands, reason);
    }

    @Test
    void FR_022_facilityThresholdEffectiveNowReplacesCurrentVersion() {
        when(store.open("FACILITY", null, TEMPERATURE))
                .thenReturn(List.of(version(10, "FACILITY", null, "EFFECTIVE", NOW.minus(Duration.ofDays(30)))));

        long id = service.set(facility(NOW.minusSeconds(20), valid, new Reason("Chỉnh theo khuyến cáo")), user);

        assertThat(id).isEqualTo(50L);
        verify(locks).lock("M06.THRESHOLD:FACILITY:" + TEMPERATURE);
        verify(store).end(10, "EXPIRED", NOW, null, null, 2);
        var inserted = ArgumentCaptor.forClass(ThresholdVersion.class);
        verify(store).insert(inserted.capture(), any(), anyLong(), any());
        assertThat(inserted.getValue().status()).isEqualTo("EFFECTIVE");
        assertThat(inserted.getValue().effectiveFrom()).isEqualTo(NOW);
    }

    @Test
    void FR_020_reasonRequiredWhenReplacingPreviousVersion() {
        when(store.open("FACILITY", null, TEMPERATURE))
                .thenReturn(List.of(version(10, "FACILITY", null, "EFFECTIVE", NOW.minus(Duration.ofDays(30)))));

        assertThatThrownBy(() -> service.set(facility(NOW, valid, null), user))
                .isInstanceOf(ReasonRequiredException.class);
        verify(store, never()).insert(any(), any(), anyLong(), any());
    }
}
