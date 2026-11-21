package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertLocationService;
import com.eldercare.backend.incident.internal.alert.service.AlertQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Danh sách cảnh báo phải kèm lệnh được phép: thẻ trên bảng cảnh báo chỉ hiện nút theo trường này (api-007 Alert). */
class FR_027_AlertListCommandsTest {
    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");

    private final AlertStore alerts = mock(AlertStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ScopeApi scopes = mock(ScopeApi.class);
    private final AccountApi accounts = mock(AccountApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final AlertQueryService service =
            new AlertQueryService(alerts, permissions, scopes, accounts, null, mock(AlertLocationService.class));

    @Test
    void FR_027_listCarriesAllowedCommandsLikeDetail() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(user.accountId()).thenReturn(900L);
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(scopes.filter(any(), any())).thenReturn(ScopeFilter.FACILITY);
        when(alerts.search(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(List.of(newAlert()));

        var rows = service.list(null, null, null, null, user);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().allowedCommands()).contains("receive");
    }

    private static AlertRecord newAlert() {
        return new AlertRecord(
                50,
                101L,
                7L,
                "OTHER_HEALTH_CARE",
                "THU_CONG:92",
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
                900L,
                "Ăn kém",
                null,
                null,
                null,
                false,
                1);
    }
}
