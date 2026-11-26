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
import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
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
 * FR-047k, BR-M05-31 (Q-376): màn sự cố mức Khẩn cấp có nút "Gọi 115" cho người có quyền xử lý sự cố, như màn cảnh báo.
 * Chi tiết sự cố phải trả {@code call-115} trong {@code allowedCommands} để màn chỉ hiện nút đúng lúc.
 */
class FR_047k_IncidentCall115CommandTest {
    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");
    private static final long INCIDENT_ID = 71;

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final IncidentProgressionStore progressions = mock(IncidentProgressionStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentQueryService service = new IncidentQueryService(
            incidents,
            progressions,
            permissions,
            mock(ScopeApi.class),
            mock(AccountApi.class),
            null,
            mock(FallMonitoringStore.class));

    private void given(Role role, IncidentLevel level, String status) {
        when(user.roles()).thenReturn(Set.of(role));
        when(user.accountId()).thenReturn(900L);
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(incidents.view(INCIDENT_ID)).thenReturn(Optional.of(incident(level, status)));
        when(progressions.handlerAccountIds(INCIDENT_ID)).thenReturn(List.of());
        when(progressions.list(INCIDENT_ID)).thenReturn(List.of());
        when(progressions.levelChanges(INCIDENT_ID)).thenReturn(List.of());
    }

    @Test
    void FR_047k_handlerSeesCall115OnOpenCriticalIncident() {
        given(Role.DOCTOR, IncidentLevel.CRITICAL, "IN_PROGRESS");
        assertThat(service.detail(INCIDENT_ID, user).allowedCommands()).contains("call-115");
    }

    @Test
    void FR_047k_noCall115BelowCriticalOrWhenClosedOrForManager() {
        given(Role.DOCTOR, IncidentLevel.MEDIUM, "IN_PROGRESS");
        assertThat(service.detail(INCIDENT_ID, user).allowedCommands()).doesNotContain("call-115");
        given(Role.DOCTOR, IncidentLevel.CRITICAL, "CLOSED");
        assertThat(service.detail(INCIDENT_ID, user).allowedCommands()).doesNotContain("call-115");
        given(Role.NURSING_HOME_MANAGER, IncidentLevel.CRITICAL, "IN_PROGRESS");
        assertThat(service.detail(INCIDENT_ID, user).allowedCommands()).doesNotContain("call-115");
    }

    private static IncidentView incident(IncidentLevel level, String status) {
        return new IncidentView(
                INCIDENT_ID,
                9001L,
                11,
                11,
                null,
                NOW,
                NOW,
                1L,
                null,
                null,
                "Khó thở cấp",
                null,
                level,
                null,
                null,
                false,
                900,
                status,
                level,
                false,
                0,
                900L,
                NOW,
                false,
                null);
    }
}
