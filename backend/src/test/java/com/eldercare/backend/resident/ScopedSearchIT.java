package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.ProfileView;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.SearchPage;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 001 T016 (US1 #6, #7; FR-007; BR-M15-01; NFR-02): tra cứu lọc theo phạm vi của 002; xem một hồ sơ ngoài phạm vi trả
 * 404, vai trò không có quyền xem trả 403 (Nhân viên bếp); tìm theo họ tên, mã hồ sơ, số giấy tờ; lọc theo trạng thái;
 * phân trang theo con trỏ.
 */
class ScopedSearchIT extends ResidentProfileTestBase {

    private SearchPage search(String q, Set<ResidentStatus> statuses, String cursor, int limit) {
        return profiles.search(q, statuses, cursor, limit);
    }

    private ProfileView created(String name) {
        return createAsOffice(noDocument(name)).profile();
    }

    @Test
    void US1_6_caregiver_seesOnlyAssignedProfiles() {
        ProfileView a = created(uniqueName());
        ProfileView b = created(uniqueName());
        permissions.assign(a.id());
        user.set(CAREGIVER);

        assertThat(search(a.profileCode(), Set.of(), null, 20).items())
                .extracting(ProfileView::id)
                .containsExactly(a.id());
        assertThat(search(b.profileCode(), Set.of(), null, 20).items()).isEmpty();
        assertThat(profiles.view(ResidentProfileService.target(a.id())).id()).isEqualTo(a.id());
    }
}
