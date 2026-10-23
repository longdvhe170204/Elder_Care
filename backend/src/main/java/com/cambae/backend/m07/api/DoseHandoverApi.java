package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.DosePendingHandoverView;
import com.cambae.backend.m07.api.dto.DoseStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * API tra cứu liều thuốc phục vụ bàn giao ca trực (LieuBanGiaoApi, FR-028, BR-M09-06, Q-307).
 * Được sử dụng bởi Module M09 (008 - Ca trực điều dưỡng).
 */
public interface DoseHandoverApi {
    /**
     * Danh sách liều cần bàn giao trong ca: Trễ, Bỏ lỡ, Từ chối, Mang theo, KSĐB chờ xem lại.
     */
    List<DosePendingHandoverView> inShift(long floorAreaId, long shiftId);

    /**
     * Tra cứu trạng thái hiện hành của tập hợp mã liều (sau khi đã tính đính chính).
     */
    Map<String, DoseStatus> currentStatuses(Collection<String> doseKeys);
}
