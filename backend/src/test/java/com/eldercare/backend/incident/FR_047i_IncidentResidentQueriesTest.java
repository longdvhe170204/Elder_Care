package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.incident.internal.incident.service.IncidentResidentQueries;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.IdentityView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Đề xuất 2026-11-23: tìm người cao tuổi khi báo khẩn trong toàn viện, không phân biệt hoa thường, dấu; ít nhất 2 ký tự;
 * tối đa 5 người; chỉ người có quyền ghi sự cố hoặc nút hỗ trợ; trả phòng, tầng đang ở.
 */
class FR_047i_IncidentResidentQueriesTest {
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ResidentApi residents = mock(ResidentApi.class);
    private final ResidentLocationApi locations = mock(ResidentLocationApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentResidentQueries queries = new IncidentResidentQueries(permissions, residents, locations);

    private void allowed() {
        when(permissions.permissions(user))
                .thenReturn(Set.of(new PermissionKey(Function.EMERGENCY_ASSIST_BUTTON, Operation.CREATE)));
    }

    private void resident(long id, String name) {
        when(residents.identity(id)).thenReturn(new IdentityView(id, "NCT-" + id, name, Optional.empty()));
        when(residents.status(id)).thenReturn(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void matchesWithoutAccentsAndReturnsRoomAndFloor() {
        allowed();
        when(residents.byStatus(any())).thenReturn(List.of(1L, 2L));
        resident(1, "Nguyễn Thị Ấn");
        resident(2, "Trần Văn Bình");
        when(locations.current(anyCollection()))
                .thenReturn(Map.of(1L, new LocationView(null, null, 7L, "Tầng 2", 20L, "P201", 3L, "G1")));

        var found = queries.search("thi an", user);

        assertThat(found).singleElement().satisfies(r -> {
            assertThat(r.fullName()).isEqualTo("Nguyễn Thị Ấn");
            assertThat(r.roomName()).isEqualTo("P201");
            assertThat(r.floorAreaId()).isEqualTo(7L);
            assertThat(r.floorName()).isEqualTo("Tầng 2");
        });
    }

    @Test
    void atMostFiveResults() {
        allowed();
        when(residents.byStatus(any())).thenReturn(List.of(1L, 2L, 3L, 4L, 5L, 6L));
        for (long id = 1; id <= 6; id++) resident(id, "Lê Văn Đức " + id);
        when(locations.current(anyCollection())).thenReturn(Map.of());

        assertThat(queries.search("duc", user)).hasSize(5);
    }

    @Test
    void shortQueryAndMissingRightAreRejected() {
        assertThatThrownBy(() -> queries.search("an", user)).isInstanceOf(PermissionDeniedException.class);
        allowed();
        assertThatThrownBy(() -> queries.search(" a ", user)).isInstanceOf(InvalidValueException.class);
    }
}
