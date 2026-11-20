package com.eldercare.backend.reporting.internal.scope.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.AreaView;
import com.eldercare.backend.roombedallocation.api.dto.FloorView;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giới hạn trường của dòng chi tiết, dùng chung cho màn hình và file xuất (FR-014, FR-054; research O-04):
 *
 * <ul>
 *   <li>người cao tuổi ngoài phạm vi hiện hành: dạng rút gọn, không mở được bản ghi từ danh sách (Q-196); trong phạm vi
 *       thì mở được nếu loại bản ghi đã nối màn ({@link #OPENABLE});
 *   <li>kết quả thuộc nhóm thông tin người xem không được xem đầy đủ: chỉ còn phần không giới hạn, ví dụ mã vật phẩm
 *       thay tên thuốc (Q-133).
 * </ul>
 *
 * Dòng rút gọn xét nhóm thông tin theo vai trò của người xem, không xét phạm vi hiện hành (FR-012: "chỉ khi vai trò của
 * người xem được xem nhóm trường đó"; {@code FieldVisibilityApi.canViewByRole}).
 */
@Service
@Transactional(readOnly = true)
public class DetailRowPolicy {

    private static final String RESIDENT = "M01.RESIDENT";

    /**
     * Loại bản ghi đã nối với màn có id của feature sở hữu (FR-009 câu cuối, Q12; 016 T021, chủ dự án duyệt 2026-11-12):
     * tiền tố {@code sourceKey} trước dấu ":" đầu tiên, id là phần ngay sau. Quyền, trường ẩn do màn đó xét; loại khác
     * chưa có màn mở theo id nên không mở được. Web giữ cùng danh sách ở {@code reportLabels.js}.
     */
    static final Set<String> OPENABLE =
            Set.of("M05.INCIDENT", "M03.ASSET", "M11.PROCUREMENT_REQUEST", "M02.WAITING_LIST");

    private final ScopeApi scopes;
    private final FieldVisibilityApi fields;
    private final ObjectProvider<ResidentApi> residents;
    private final ObjectProvider<LocationStructureApi> locations;

    public DetailRowPolicy(
            ScopeApi scopes,
            FieldVisibilityApi fields,
            ObjectProvider<ResidentApi> residents,
            ObjectProvider<LocationStructureApi> locations) {
        this.scopes = scopes;
        this.fields = fields;
        this.residents = residents;
        this.locations = locations;
    }

    public List<DetailRow> apply(CurrentUser user, List<DetailRow> rows) {
        Names names = new Names(residents.getIfAvailable(), locations.getIfAvailable());
        // FR-010, FR-012: phạm vi toàn viện ở dòng "Dashboard, báo cáo" thì mọi người cao tuổi trong phạm vi; inScope
        // chỉ
        // xét phân công hiện hành nên chỉ dùng cho vai trò theo tầng, phân công (Q-196)
        boolean facilityWide = scopes.filter(user, ReportScopeService.KEY).facilityWide();
        return rows.stream()
                .map(row -> apply(user, names.fill(row), facilityWide))
                .toList();
    }

    /**
     * Họ tên người cao tuổi, tên tầng/khu cho dòng chỉ có mã (FR-009, FR-012; Q-196: dòng rút gọn vẫn có họ tên): tra ở
     * 001 ({@code ResidentApi.identity}, phần nhận dạng tối thiểu được phép cho người ngoài phạm vi, 002 FR-044a) và 003,
     * mỗi mã một lần trong lượt; không tra được thì để trống.
     */
    private static final class Names {
        private final ResidentApi residentApi;
        private final LocationStructureApi locationApi;
        private final Map<Long, String> residentNames = new HashMap<>();
        private final Map<Long, String> floorNames = new HashMap<>();
        private Set<Long> knownResidents;

        Names(ResidentApi residentApi, LocationStructureApi locationApi) {
            this.residentApi = residentApi;
            this.locationApi = locationApi;
        }

        DetailRow fill(DetailRow row) {
            String resident = row.resident() != null || row.residentId() == null
                    ? row.resident()
                    : residentNames.computeIfAbsent(row.residentId(), this::residentName);
            String floor = row.floorArea() != null || row.floorAreaId() == null
                    ? row.floorArea()
                    : floorNames.computeIfAbsent(row.floorAreaId(), this::floorName);
            return resident == row.resident() && floor == row.floorArea() ? row : row.withNames(floor, resident);
        }

        private String residentName(long residentId) {
            if (residentApi == null) {
                return null;
            }
            // identity() báo lỗi với mã không có hồ sơ và lỗi đó đánh dấu hủy cả giao dịch đọc của báo cáo dù đã bắt,
            // nên
            // chỉ tra mã có hồ sơ (mọi trạng thái), lấy danh sách một lần mỗi lượt
            if (knownResidents == null) {
                knownResidents = new HashSet<>(residentApi.byStatus(EnumSet.allOf(ResidentStatus.class)));
            }
            return knownResidents.contains(residentId)
                    ? residentApi.identity(residentId).fullName()
                    : null;
        }

        private String floorName(long floorAreaId) {
            if (locationApi == null) {
                return null;
            }
            return locationApi
                    .floor(floorAreaId)
                    .map(FloorView::name)
                    .or(() -> locationApi.area(floorAreaId).map(AreaView::name))
                    .orElse(null);
        }
    }

    private DetailRow apply(CurrentUser user, DetailRow row, boolean facilityWide) {
        DetailRow result = row;
        boolean reduced = row.residentId() != null
                && !facilityWide
                && !scopes.inScope(
                        user, new Target(RESIDENT, String.valueOf(row.residentId()), row.residentId(), null));
        if (reduced) {
            result = result.asReduced();
        } else {
            result = result.withCanOpenRecord(OPENABLE.contains(row.sourceKey().split(":", 2)[0]));
        }
        if (row.resultGroup() != null) {
            Visibility level = reduced
                    ? fields.canViewByRole(user, row.resultGroup())
                    : fields.canView(
                            user, row.resultGroup(), new Target(null, null, row.residentId(), row.floorAreaId()));
            if (level != Visibility.FULL) {
                result = result.withResult(row.restrictedResult());
            }
        }
        return result;
    }
}
