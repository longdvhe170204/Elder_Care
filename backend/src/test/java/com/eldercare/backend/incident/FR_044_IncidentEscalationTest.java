package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.incident.api.events.AlertEscalated;
import com.eldercare.backend.incident.api.events.MaximumEscalationReminder;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentEscalationState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentEscalationService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-044, FR-033, FR-034 (chủ dự án chốt 2026-11-07, team-plan dòng T035): sự cố quá hạn tiếp nhận dùng chung
 * {@code AlertEscalated}, {@code MaximumEscalationReminder} với cảnh báo, phân biệt bằng loại nguồn {@code M05.INCIDENT}.
 */
class FR_044_IncidentEscalationTest {
    private static final Instant NOW = Instant.parse("2026-11-08T03:00:00Z");

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final ParameterApi parameters = mock(ParameterApi.class);
    private final AlertStaffPort staff = mock(AlertStaffPort.class);
    private final TransactionTemplate transaction = mock(TransactionTemplate.class);
    private final List<Object> published = new ArrayList<>();
    private final IncidentEscalationService service =
            new IncidentEscalationService(incidents, published::add, parameters, transaction, staff);

    @SuppressWarnings("unchecked")
    private void setUp() {
        when(transaction.execute(any()))
                .thenAnswer(call -> ((TransactionCallback<Object>) call.getArgument(0)).doInTransaction(null));
        var interval = mock(ParameterValue.class);
        when(interval.asDuration()).thenReturn(Duration.ofMinutes(15));
        when(parameters.read(ParameterCode.CFG_M05_01)).thenReturn(interval);
        when(staff.onDutyDoctors(NOW)).thenReturn(List.of(9L));
    }

    private static IncidentEscalationState state(int level, Instant maxAt) {
        return new IncidentEscalationState(
                41, 101L, 7L, "NEW", IncidentLevel.MEDIUM, level, NOW.minusSeconds(60), maxAt, 0, false, 3);
    }

    @Test
    void FR_044_escalationAndMaximumReminderUseSharedEventsWithIncidentSource() {
        setUp();
        when(incidents.lockForEscalation(41)).thenReturn(Optional.of(state(0, null)));
        service.escalate(41, NOW);

        assertThat(published).singleElement().isInstanceOfSatisfying(AlertEscalated.class, event -> {
            assertThat(event.sourceType()).isEqualTo("M05.INCIDENT");
            assertThat(event.sourceId()).isEqualTo(41L);
            assertThat(event.escalationLevel()).isEqualTo(1);
            assertThat(event.notificationRequests()).hasSize(1);
        });

        published.clear();
        when(incidents.lockForEscalation(41)).thenReturn(Optional.of(state(2, NOW.minusSeconds(600))));
        service.escalate(41, NOW);

        assertThat(published).singleElement().isInstanceOfSatisfying(MaximumEscalationReminder.class, event -> {
            assertThat(event.sourceType()).isEqualTo("M05.INCIDENT");
            assertThat(event.sourceId()).isEqualTo(41L);
            assertThat(event.sequence()).isEqualTo(1);
        });
    }
}
