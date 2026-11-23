package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.HandoverCandidateView;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.emergency.service.HandoverStaffService;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * FR-049 (lệnh Chuyển viện bắt buộc người bàn giao ở 004): danh sách chọn người bàn giao lấy từ nhân viên đang trong ca ở tầng
 * của người cao tuổi, không theo ca của người lập lệnh (Bác sĩ trực toàn viện không chung ca với tầng).
 */
class FR_049_HandoverStaffTest {
    private static final Instant NOW = Instant.parse("2026-11-22T03:00:00Z");
    private static final long RESIDENT = 9001;
    private static final long FLOOR = 11;

    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ResidentLocationApi locations = mock(ResidentLocationApi.class);
    private final AlertStaffPort staff = mock(AlertStaffPort.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final HandoverStaffService service = new HandoverStaffService(permissions, locations, staff, time);

    private void given(Role role) {
        when(user.roles()).thenReturn(Set.of(role));
        when(time.now()).thenReturn(NOW);
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(locations.current(RESIDENT))
                .thenReturn(Optional.of(new LocationView(null, null, FLOOR, "Tầng 1", 5L, "P101", 7L, "G1")));
    }

    @Test
    void FR_049_doctorSeesWorkingStaffOnShiftAtResidentFloor() {
        given(Role.DOCTOR);
        when(staff.handoverCandidates(FLOOR, NOW))
                .thenReturn(List.of(
                        new HandoverCandidateView(101, "Hoàng Thị Dung", "Điều dưỡng"),
                        new HandoverCandidateView(102, "Vũ Văn Cường", "Nhân viên chăm sóc")));

        var result = service.forResident(RESIDENT, user);

        assertThat(result).extracting(HandoverStaffService.Candidate::id).containsExactly(101L, 102L);
        assertThat(result.getFirst().fullName()).isEqualTo("Hoàng Thị Dung");
        assertThat(result.getFirst().position()).isEqualTo("Điều dưỡng");
    }

    @Test
    void FR_049_noCurrentLocationMeansEmptyList() {
        given(Role.NURSING_STAFF);
        when(locations.current(RESIDENT)).thenReturn(Optional.empty());
        assertThat(service.forResident(RESIDENT, user)).isEmpty();
    }

    @Test
    void FR_049_onlyNurseOrDoctorMayAsk() {
        given(Role.FLOOR_MANAGER);
        assertThatThrownBy(() -> service.forResident(RESIDENT, user)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_049_outOfScopeIsNotFound() {
        given(Role.DOCTOR);
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.SCOPE));
        assertThatThrownBy(() -> service.forResident(RESIDENT, user)).isInstanceOf(NotFoundException.class);
    }
}
