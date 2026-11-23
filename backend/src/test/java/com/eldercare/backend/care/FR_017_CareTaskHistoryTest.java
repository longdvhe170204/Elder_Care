package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.task.domain.CareTaskHistoryFilter;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskHistory;
import com.eldercare.backend.common.error.PermissionDeniedException;
import java.util.*;
import org.junit.jupiter.api.*;

/** Lịch sử công việc đã lập không theo ca (màn Lập công việc ca, FR-017): quyền, phạm vi, phân trang. */
class FR_017_CareTaskHistoryTest {
    static final PermissionKey VIEW = new PermissionKey(Function.SHIFT_TASK_PLANNING, Operation.VIEW);
    final ScopeFilter floor3 = new ScopeFilter(false, Set.of(3L), Set.of(), Set.of());
    CareTaskStore tasks;
    ScopeApi scopes;
    PermissionApi permissions;
    CareTaskHistory history;

    @BeforeEach
    void setUp() {
        tasks = mock(CareTaskStore.class);
        scopes = mock(ScopeApi.class);
        permissions = mock(PermissionApi.class);
        history = new CareTaskHistory(tasks, scopes, permissions, mock(CurrentUser.class));
        when(permissions.permissions(any())).thenReturn(Set.of(VIEW));
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(scopes.filter(any(), eq(VIEW))).thenReturn(floor3);
        when(tasks.history(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        when(tasks.historyCount(any(), any())).thenReturn(42L);
    }

    @Test
    void khongCoQuyenXemLapCongViec_thiChan() {
        when(permissions.permissions(any())).thenReturn(Set.of());
        assertThatThrownBy(() -> history.list(null, null, null, null, 0, 20))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void locTangNgoaiPhamVi_thiChan() {
        when(permissions.hasPermission(any(), eq(VIEW), argThat(t -> Objects.equals(t.floorAreaId(), 9L))))
                .thenReturn(new PermissionResult(false, Optional.empty(), Optional.empty(), Optional.empty()));
        assertThatThrownBy(() -> history.list(9L, null, null, null, 0, 20))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void phanTrang_vaApPhamViCuaNguoiGoi_kemBoLoc() {
        var page = history.list(3L, 77L, 9L, 22L, 2, 20);
        assertThat(page.total()).isEqualTo(42);
        verify(tasks).history(new CareTaskHistoryFilter(3L, 77L, 9L, 22L), floor3, 40, 20);
    }

    @Test
    void coTrangQuaLon_thiGioiHan100Viec() {
        history.list(null, null, null, null, -1, 5000);
        verify(tasks).history(new CareTaskHistoryFilter(null, null, null, null), floor3, 0, 100);
    }
}
