package com.eldercare.backend.notification.api;

import com.eldercare.backend.notification.api.dto.NotifiedPersonView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Tra cứu thông báo đã gửi ({@code ThongBaoTraCuuApi}; 009 contracts mục 1.2); chỉ đọc. */
public interface NotificationLookupApi {

    /**
     * Người đã được thông báo về một bản ghi nguồn (FR-035): căn cứ, trạng thái, kết quả từng kênh, thời điểm gửi, xem,
     * xác nhận và cách, lượt nhắc gọi và kết quả, dấu thiếu mức.
     */
    List<NotifiedPersonView> bySource(String sourceRecordType, String sourceRecordId);

    /** Thời điểm người nhận xem lần đầu (BR-M10-06). */
    Optional<Instant> viewedAt(long notificationId);
}
