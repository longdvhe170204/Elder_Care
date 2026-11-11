package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.roombedallocation.api.dto.AssetFlag;
import com.eldercare.backend.roombedallocation.api.dto.OverdueAssetView;
import com.eldercare.backend.roombedallocation.api.dto.VehicleScheduleView;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 019 contracts mục 2.3 (TaiSanThongKeApi) cho 016 (TS.QUA_HAN, 18.5), theo yêu cầu chung của 016 module-interfaces
 * mục 2 (chủ dự án duyệt 2026-11-06): nhận phạm vi, chỉ đọc, không khóa, có hàm đếm cho dashboard.
 */
public interface AssetStatisticsApi {
    /** Dấu hiện tại có dueOn trước on; phạm vi theo tầng hiện tại, không tái dựng dấu quá khứ. */
    Stream<OverdueAssetView> overdue(ScopeFilter scope, LocalDate on);

    /** Đếm tài sản theo dấu quá hạn tại ngày, không tải bản ghi. */
    Map<AssetFlag, Long> countOverdue(ScopeFilter scope, LocalDate on);

    /**
     * Tài sản chưa thanh lý có hạn bảo trì, đăng kiểm, bảo hiểm trong [on, on + CFG-M03-09] và chưa mang dấu quá hạn của
     * hạn đó (mốc nhắc BR-M03-17; 016 FR-020, T008). {@code flag} là dấu sẽ gắn khi qua hạn, {@code dueOn} là hạn.
     */
    Stream<OverdueAssetView> dueSoon(ScopeFilter scope, LocalDate on);

    /** Lịch xe có giờ đi dự kiến trong [from, to] theo ngày nghiệp vụ; toàn viện (M03.VEHICLE_SCHEDULE). */
    Stream<VehicleScheduleView> vehicleSchedules(LocalDate from, LocalDate to);
}
