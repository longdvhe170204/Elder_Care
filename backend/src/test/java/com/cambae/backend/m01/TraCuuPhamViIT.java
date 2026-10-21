package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService.ProfileView;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService.SearchPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 001 T016 (US1 #6, #7; FR-007; BR-M15-01; NFR-02): tra cứu lọc theo phạm vi của 002; xem một hồ sơ ngoài phạm vi trả
 * 404, vai trò không có quyền xem trả 403 (Nhân viên bếp); tìm theo họ tên, mã hồ sơ, số giấy tờ; lọc theo trạng thái;
 * phân trang theo con trỏ.
 */
class TraCuuPhamViIT extends ResidentProfileTestBase {

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

    @Test
    void US1_6_viewOfAProfileOutsideScope_isNotFound() {
        ProfileView outside = created(uniqueName());
        user.set(CAREGIVER);

        assertThatThrownBy(() -> profiles.view(ResidentProfileService.target(outside.id())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void US1_7_kitchenStaff_isDeniedSearchAndView() {
        ProfileView any = created(uniqueName());
        user.set(KITCHEN);

        assertThatThrownBy(() -> search(null, Set.of(), null, 20)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> profiles.view(ResidentProfileService.target(any.id())))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_007_officeAndDoctor_searchWholeFacility() {
        ProfileView any = created(uniqueName());

        for (var viewer : List.of(OFFICE, DOCTOR, MANAGER)) {
            user.set(viewer);
            assertThat(search(any.profileCode(), Set.of(), null, 20).items())
                    .as(viewer.roles().toString())
                    .extracting(ProfileView::id)
                    .containsExactly(any.id());
        }
    }

    @Test
    void FR_007_search_matchesNameCodeAndDocumentNumber() {
        String cccd = uniqueCitizenId();
        String name = "Trương Thị Kim " + cccd;
        ProfileView profile = createAsOffice(withCitizenId(name, cccd)).profile();
        user.set(OFFICE);

        assertThat(search(cccd, Set.of(), null, 20).items())
                .extracting(ProfileView::id)
                .containsExactly(profile.id());
        assertThat(search(profile.profileCode(), Set.of(), null, 20).items())
                .extracting(ProfileView::id)
                .containsExactly(profile.id());
        // Tên: không phân biệt hoa thường
        assertThat(search("trương thị kim " + cccd, Set.of(), null, 20).items())
                .extracting(ProfileView::id)
                .containsExactly(profile.id());
        assertThat(search("khong-ton-tai-" + cccd, Set.of(), null, 20).items()).isEmpty();
    }

    @Test
    void FR_007_search_treatsPercentAndUnderscoreAsPlainText() {
        created(uniqueName());
        user.set(OFFICE);

        assertThat(search("%", Set.of(), null, 20).items()).isEmpty();
        assertThat(search("_", Set.of(), null, 20).items()).isEmpty();
    }

    @Test
    void FR_007_search_filtersByStatus() {
        String stem = uniqueCitizenId();
        ProfileView admission = created("Lọc Trạng Thái " + stem + " A");
        ProfileView resident = created("Lọc Trạng Thái " + stem + " B");
        setStatus(resident.id(), "IN_RESIDENCE");
        user.set(OFFICE);

        assertThat(search(stem, Set.of(ResidentStatus.IN_RESIDENCE), null, 20).items())
                .extracting(ProfileView::id)
                .containsExactly(resident.id());
        assertThat(search(stem, Set.of(ResidentStatus.ADMISSION_IN_PROGRESS), null, 20)
                        .items())
                .extracting(ProfileView::id)
                .containsExactly(admission.id());
        assertThat(search(stem, Set.of(), null, 20).items()).hasSize(2);
    }

    @Test
    void NFR_02_search_pagesByCursorWithoutRepeatsOrGaps() {
        String stem = uniqueCitizenId();
        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            expected.add(created("Phân Trang " + stem + " " + i).id());
        }
        user.set(OFFICE);

        List<Long> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            SearchPage page = search(stem, Set.of(), cursor, 2);
            assertThat(page.items()).hasSizeLessThanOrEqualTo(2);
            page.items().forEach(p -> seen.add(p.id()));
            cursor = page.nextCursor();
            pages++;
        } while (cursor != null && pages < 10);

        assertThat(pages).isEqualTo(3);
        assertThat(seen).containsExactlyInAnyOrderElementsOf(expected).doesNotHaveDuplicates();
    }

    @Test
    void NFR_02_search_limitIsBounded() {
        user.set(OFFICE);

        assertThat(search(null, Set.of(), null, 1000).items()).hasSizeLessThanOrEqualTo(100);
        assertThat(search(null, Set.of(), null, 0).items()).hasSizeLessThanOrEqualTo(100);
    }
}
