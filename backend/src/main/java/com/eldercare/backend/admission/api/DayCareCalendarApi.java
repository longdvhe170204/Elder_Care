package com.eldercare.backend.admission.api;

import com.eldercare.backend.admission.api.dto.DayCareCalendarView;
import java.time.LocalDate;

/**
 * Lịch bán trú đã quyết định của 004 cho 005 (tên logic {@code LichBanTruApi}; FR-060, FR-060a). Chỉ đọc; adapter {@code
 * DayCareCalendarPort} thuộc 005 gọi hàm này (chiều 005 → 004).
 */
public interface DayCareCalendarApi {

    /** Ngày khu bán trú nghỉ Hiệu lực và mọi báo vắng đã ghi của người cao tuổi ngày D, theo thời điểm ghi. */
    DayCareCalendarView onDate(long residentId, LocalDate date);
}
