package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.ReviewRecipient;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.scope.service.DetailRowPolicy;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** Giới hạn trường của dòng chi tiết (016 T005; research O-04): FR-012 (Q-196), FR-014, FR-054 (Q-133). */
class DetailRowPolicyTest {

    private static final CurrentUser USER = new ReportScopeTest.User(Set.of(Role.FLOOR_MANAGER));

    private final Set<Long> residentsInScope = Set.of(501L);
    private Visibility medication = Visibility.FULL;
    private boolean facilityWide = false;

    private final ScopeApi scopes = new ScopeApi() {
        @Override
        public boolean inScope(CurrentUser user, Target target) {
            return residentsInScope.contains(target.residentId());
        }

        @Override
        public ScopeFilter filter(CurrentUser user, PermissionKey key) {
            return facilityWide ? ScopeFilter.FACILITY : new ScopeFilter(false, Set.of(), Set.of(), residentsInScope);
        }

        @Override
        public ScopeFilter assignedFilter(CurrentUser user, Set<AssignmentKind> kinds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScopeFilter historyFilter(CurrentUser user, PermissionKey key, Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Long> shiftLead(long floorAreaId, Instant at) {
            return Optional.empty();
        }

        @Override
        public Optional<Long> assignedFloorManager(long floorAreaId) {
            return Optional.empty();
        }

        @Override
        public ReviewRecipient reviewDecider(Optional<Long> floorAreaId) {
            throw new UnsupportedOperationException();
        }
    };

    private Visibility medicationByRole = Visibility.FULL;

    private final FieldVisibilityApi fields = new FieldVisibilityApi() {
        @Override
        public Visibility canView(CurrentUser user, InfoGroup group, Target target) {
            return group == InfoGroup.MEDICATION ? medication : Visibility.FULL;
        }

        @Override
        public Visibility canViewByRole(CurrentUser user, InfoGroup group) {
            return group == InfoGroup.MEDICATION ? medicationByRole : Visibility.FULL;
        }
    };

    private static DetailRow dose(long residentId) {
        return row("M07.DOSE:d1", residentId);
    }

    private static DetailRow row(String sourceKey, long residentId) {
        return new DetailRow(
                sourceKey,
                "006",
                "DOSE",
                "MISSED",
                Instant.parse("2026-10-05T01:00:00Z"),
                1L,
                "Tầng 1",
                residentId,
                "Nguyễn Văn A",
                "ĐD B",
                Map.of("medication", "Amlodipin 5 mg", "itemCode", "VT-001"),
                InfoGroup.MEDICATION,
                Map.of("itemCode", "VT-001"),
                false,
                false,
                false);
    }

    private final FakeResidents residents = new FakeResidents();
    private final FakeLocations locations = new FakeLocations();

    private static <T> ObjectProvider<T> provider(Class<T> type, T bean) {
        return new StaticListableBeanFactory(Map.of("bean", bean)).getBeanProvider(type);
    }

    private DetailRowPolicy policy() {
        return new DetailRowPolicy(
                scopes,
                fields,
                provider(ResidentApi.class, residents),
                provider(LocationStructureApi.class, locations));
    }

    /** Dòng của chỉ tiêu chỉ có mã người cao tuổi, mã tầng (ví dụ {@code DetailRow.simple}). */
    private static DetailRow idsOnly(String sourceKey, long residentId, long floorAreaId) {
        return DetailRow.simple(
                sourceKey, "001", "RESIDENT", "ADMISSION_IN_PROGRESS", null, floorAreaId, residentId, Map.of());
    }

    /**
     * FR-009, FR-012 (Q-196): dòng chi tiết hiện họ tên người cao tuổi và tầng/khu, cả dòng rút gọn ngoài phạm vi hiện
     * hành; nguồn chỉ trả mã thì tra họ tên ở 001, tên tầng ở 003. Không tra được thì để trống, không làm hỏng danh sách.
     */
    @Test
    void FR_012_Q_196_namesFilledFromIds_alsoOnReducedRows() {
        residents.resident(501, "Nguyễn Văn An", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        residents.resident(777, "Trần Thị Bình", Gender.FEMALE, ResidentStatus.ADMISSION_IN_PROGRESS);
        locations.floor(9001, null);

        List<DetailRow> rows = policy().apply(
                        USER,
                        List.of(
                                idsOnly("M01.RESIDENT:501", 501, 9001),
                                idsOnly("M01.RESIDENT:777", 777, 9001),
                                idsOnly("M01.RESIDENT:888", 888, 4242),
                                dose(501)));

        assertThat(rows)
                .extracting(DetailRow::resident)
                .containsExactly("Nguyễn Văn An", "Trần Thị Bình", null, "Nguyễn Văn A");
        assertThat(rows).extracting(DetailRow::reduced).containsExactly(false, true, true, false);
        assertThat(rows)
                .extracting(DetailRow::floorArea)
                .containsExactly("[Tầng kiểm thử]", "[Tầng kiểm thử]", null, "Tầng 1");
    }

    @Test
    void FR_009_residentInScope_rowUnchanged() {
        DetailRow row = policy().apply(USER, List.of(dose(501))).getFirst();

        assertThat(row.reduced()).isFalse();
        assertThat(row.result()).containsEntry("medication", "Amlodipin 5 mg");
    }

    /**
     * FR-009 câu cuối, Q12 (016 T021; chủ dự án duyệt 2026-11-12): chỉ loại bản ghi đã nối với màn có id của feature sở hữu
     * mới mở được; quyền, trường ẩn do màn đó xét. Liều thuốc chưa có màn mở theo id.
     */
    @Test
    void FR_009_Q12_onlyLinkedRecordTypesCanOpen() {
        List<DetailRow> rows = policy().apply(
                        USER,
                        List.of(
                                row("M05.INCIDENT:9", 501),
                                row("M03.ASSET:4:MAINTENANCE", 501),
                                row("M11.PROCUREMENT_REQUEST:7", 501),
                                row("M02.WAITING_LIST:3", 501),
                                dose(501),
                                row("M05.INCIDENT:10", 777)));

        assertThat(rows).extracting(DetailRow::canOpenRecord).containsExactly(true, true, true, true, false, false);
    }

    /**
     * FR-010, FR-012 (Q-196): người xem có phạm vi toàn viện ở dòng "Dashboard, báo cáo" (Quản lý viện, Hành chính, Bác
     * sĩ) thấy mọi người cao tuổi trong phạm vi, không có dòng rút gọn; phạm vi hiện hành theo phân công chỉ xét với vai
     * trò theo tầng, phân công.
     */
    @Test
    void FR_012_facilityWideViewer_noReducedRows() {
        facilityWide = true;

        List<DetailRow> rows = policy().apply(USER, List.of(row("M05.INCIDENT:9", 777), dose(501)));

        assertThat(rows).extracting(DetailRow::reduced).containsExactly(false, false);
        assertThat(rows).extracting(DetailRow::canOpenRecord).containsExactly(true, false);
    }

    @Test
    void FR_012_Q_196_reducedRow_resultByRoleNotByCurrentScope() {
        medication = Visibility.NONE; // ngoài phạm vi hiện hành thì canView không cho
        assertThat(policy().apply(USER, List.of(dose(777))).getFirst().result())
                .containsEntry("medication", "Amlodipin 5 mg");

        medicationByRole = Visibility.NONE;
        assertThat(policy().apply(USER, List.of(dose(777))).getFirst().result())
                .containsExactly(Map.entry("itemCode", "VT-001"));
    }
}
