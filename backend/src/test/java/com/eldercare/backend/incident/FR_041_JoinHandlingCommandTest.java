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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-041: "Tham gia xử lý" ghi tên vào danh sách người xử lý, nên không trả cho người đã có tên hoặc đang phụ trách. */
class FR_041_JoinHandlingCommandTest {
    private static final Instant NOW = Instant.parse("2026-11-23T08:00:00Z");

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

    @BeforeEach
    void defaults() {
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.DOCTOR));
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
    }

    private Set<String> commands(Long assignee) {
        when(incidents.view(41))
                .thenReturn(Optional.of(new IncidentView(
                        41,
                        101L,
                        1,
                        1,
                        null,
                        null,
                        NOW,
                        7L,
                        null,
                        null,
                        "Ngã",
                        null,
                        IncidentLevel.CRITICAL,
                        null,
                        null,
                        false,
                        5L,
                        "IN_PROGRESS",
                        IncidentLevel.CRITICAL,
                        true,
                        0,
                        assignee,
                        NOW,
                        false,
                        null)));
        return service.detail(41, user).allowedCommands();
    }

    @Test
    void FR_041_otherStaffCanJoin() {
        assertThat(commands(5L)).contains("join-handling");
    }

    @Test
    void FR_041_currentAssigneeHasNoJoin() {
        assertThat(commands(900L)).doesNotContain("join-handling").contains("progressions");
    }

    @Test
    void FR_041_listedHandlerHasNoJoin() {
        when(progressions.handlerAccountIds(41)).thenReturn(List.of(900L));
        assertThat(commands(5L)).doesNotContain("join-handling");
    }
}
