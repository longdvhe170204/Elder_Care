package com.eldercare.backend.care.api.spi;

import java.time.Instant;

/** 007 cài; MANDATORY. FR-046–048: lỗi nguồn phải rollback lệnh chuyến. */
public interface TripIncidentPort {
    /** Sự cố đi lạc hoặc không trở về, Khẩn cấp, nguồn hoạt động ngoài viện; trả mã sự cố. */
    long recordMissing(
            long residentId, long tripId, Long leaderId, String lastSeenPlace, Instant lastSeenAt, String description);

    /** Ghi diễn biến đã tìm thấy, đã trở về; không tự đóng sự cố. */
    void recordFoundReturned(long incidentId, long residentId, long tripId, Instant at);
}
