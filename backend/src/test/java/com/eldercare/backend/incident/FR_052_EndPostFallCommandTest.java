package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.FallMonitoringContent;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * FR-052, BR-M05-07: chỉ Bác sĩ kết thúc sớm khoảng theo dõi sau ngã, và chỉ khi sự cố này đang có khoảng theo dõi; chi
 * tiết sự cố phải trả lệnh này trong {@code allowedCommands} để màn chỉ hiện nút đúng lúc.
 */
class FR_052_EndPostFallCommandTest {
    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");
    private static final long INCIDENT_ID = 70;
    private static final long RESIDENT_ID = 9001;

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final IncidentProgressionStore progressions = mock(IncidentProgressionStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final FallMonitoringStore windows = mock(FallMonitoringStore.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentQueryService service = new IncidentQueryService(
            incidents, progressions, permissions, mock(ScopeApi.class), mock(AccountApi.class), null, windows);

    private void given(Role role, Long windowIncidentId) {
        when(user.roles()).thenReturn(Set.of(role));
        when(user.accountId()).thenReturn(900L);
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(incidents.view(INCIDENT_ID)).thenReturn(Optional.of(fallIncident()));
        when(progressions.handlerAccountIds(INCIDENT_ID)).thenReturn(List.of());
        when(progressions.list(INCIDENT_ID)).thenReturn(List.of());
        when(progressions.levelChanges(INCIDENT_ID)).thenReturn(List.of());
        when(windows.active(RESIDENT_ID))
                .thenReturn(windowIncidentId == null ? Optional.empty() : Optional.of(window(windowIncidentId)));
    }

    @Test
    void FR_052_doctorSeesEndCommandWhileThisIncidentHasActiveWindow() {
        given(Role.DOCTOR, INCIDENT_ID);
        assertThat(service.detail(INCIDENT_ID, user).allowedCommands()).contains("end-post-fall-monitoring");
    }

    @Test
    void FR_052_nurseDoesNotSeeEndCommand() {
        given(Role.NURSING_STAFF, INCIDENT_ID);
        assertThat(service.detail(INCIDENT_ID, user).allowedCommands()).doesNotContain("end-post-fall-monitoring");
    }

    @Test
    void FR_052_noCommandWhenWindowBelongsToAnotherIncidentOrNoWindow() {
        given(Role.DOCTOR, INCIDENT_ID + 1);
        assertThat(service.detail(INCIDENT_ID, user).allowedCommands()).doesNotContain("end-post-fall-monitoring");
        given(Role.DOCTOR, null);
        assertThat(service.detail(INCIDENT_ID, user).allowedCommands()).doesNotContain("end-post-fall-monitoring");
    }

    private static SimpleRecord<FallMonitoringContent> window(long incidentId) {
        return new SimpleRecord<>(
                5,
                RESIDENT_ID,
                null,
                "ACTIVE",
                new FallMonitoringContent(incidentId, NOW, NOW.plusSeconds(86_400), 60, null, null),
                null,
                null,
                null,
                null,
                NOW,
                900,
                0);
    }

    private static IncidentView fallIncident() {
        return new IncidentView(
                INCIDENT_ID,
                RESIDENT_ID,
                11,
                11,
                null,
                NOW,
                NOW,
                1L,
                null,
                null,
                "Ngã trong phòng tắm",
                null,
                IncidentLevel.MEDIUM,
                null,
                null,
                false,
                900,
                "IN_PROGRESS",
                IncidentLevel.MEDIUM,
                false,
                0,
                900L,
                NOW,
                false,
                null);
    }
}
