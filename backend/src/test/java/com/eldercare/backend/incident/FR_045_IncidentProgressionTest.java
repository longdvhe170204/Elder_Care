package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentProgressionService;
import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.dto.EndOfLifeWishView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Ghi diễn biến sự cố (bảng FR-045 dòng "Ghi diễn biến", FR-048a, DBR-19, Q-71). */
class FR_045_IncidentProgressionTest {
    private static final Instant NOW = Instant.parse("2026-11-04T02:00:00Z");
    private static final long ACTION = 5L;
    private static final long CPR = 6L;

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final IncidentProgressionStore progressions = mock(IncidentProgressionStore.class);
    private final IncidentCatalogService catalog = mock(IncidentCatalogService.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final EndOfLifeWishApi wishes = mock(EndOfLifeWishApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentProgressionService service =
            new IncidentProgressionService(incidents, progressions, catalog, permissions, audit, time, wishes);

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(permissions.hasPermission(eq(user), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(catalog.progressionTypes())
                .thenReturn(List.of(
                        new ProgressionTypeView(ACTION, "ACTION_TAKEN", "Hành động", ProgressionGroup.ACTION, false, 0),
                        new ProgressionTypeView(CPR, "CHEST_COMPRESSION", "Ép tim", ProgressionGroup.ACTION, true, 0)));
        when(incidents.lock(41)).thenReturn(Optional.of(state("IN_PROGRESS")));
        when(incidents.recordedBy(41)).thenReturn(800L);
        when(wishes.current(101L)).thenReturn(Optional.empty());
    }

    private static IncidentState state(String status) {
        return new IncidentState(41, 101L, 7L, status, IncidentLevel.CRITICAL, true, 3);
    }

    @Test
    void FR_045_staffInScopeAddsProgression() {
        service.add(41, ACTION, "Đã sơ cứu vết thương", null, user);

        verify(progressions).add(41, ACTION, "ACTION_TAKEN", "Đã sơ cứu vết thương", NOW, 900L, false, 7L, NOW);
        verify(audit).write(any());
    }

    @Test
    void Q_71_managerMayNotAddProgression() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));

        assertThatThrownBy(() -> service.add(41, ACTION, "Ghi chú", null, user))
                .isInstanceOf(PermissionDeniedException.class);
        verifyNothingAdded();
    }

    @Test
    void FR_046_closedIncidentDoesNotTakeProgression() {
        when(incidents.lock(41)).thenReturn(Optional.of(state("CLOSED")));

        assertThatThrownBy(() -> service.add(41, ACTION, "Ghi chú", null, user)).isInstanceOf(BusinessException.class);
        verifyNothingAdded();
    }

    @Test
    void DBR_19_resuscitationBlockedUntilWishCheckedWhenWishExists() {
        var wish = mock(EndOfLifeWishView.class);
        when(wishes.current(101L)).thenReturn(Optional.of(wish));
        when(progressions.wishChecked(41)).thenReturn(false);

        assertThatThrownBy(() -> service.add(41, CPR, "Ép tim 2 phút", null, user))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("WISH_NOT_CHECKED");
                    assertThat(e.properties()).containsEntry("endOfLifeWish", wish);
                });
        verifyNothingAdded();

        when(progressions.wishChecked(41)).thenReturn(true);
        service.add(41, CPR, "Ép tim 2 phút", null, user);
        verify(progressions).add(41, CPR, "CHEST_COMPRESSION", "Ép tim 2 phút", NOW, 900L, false, 7L, NOW);
    }

    @Test
    void FR_048a_noWishMeansNoCheckRequired() {
        service.add(41, CPR, "Ép tim", NOW.minusSeconds(60), user);

        verify(progressions).add(41, CPR, "CHEST_COMPRESSION", "Ép tim", NOW.minusSeconds(60), 900L, false, 7L, NOW);
    }

    @Test
    void FR_045_recorderOutsideScopeMayStillAddProgression() {
        when(user.accountId()).thenReturn(800L);
        when(permissions.hasPermission(eq(user), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.SCOPE));

        service.add(41, ACTION, "Đã gọi điều dưỡng", null, user);

        verify(progressions).add(41, ACTION, "ACTION_TAKEN", "Đã gọi điều dưỡng", NOW, 800L, true, 7L, NOW);
    }

    @Test
    void FR_045_otherStaffOutsideScopeSeesNotFound() {
        when(permissions.hasPermission(eq(user), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.SCOPE));

        assertThatThrownBy(() -> service.add(41, ACTION, "Ghi chú", null, user)).isInstanceOf(NotFoundException.class);
        verifyNothingAdded();
    }

    private void verifyNothingAdded() {
        verify(progressions, never())
                .add(anyLong(), anyLong(), any(), any(), any(), anyLong(), anyBoolean(), any(), any());
    }
}
