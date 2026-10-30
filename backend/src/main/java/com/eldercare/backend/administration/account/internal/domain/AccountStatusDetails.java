package com.eldercare.backend.administration.account.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Chi tiết một lần chuyển trạng thái tài khoản ở {@code status_history} (002 T098, data-model 002 mục 0.1): dòng
 * {@code POINT} của {@code M15.ACCOUNT}, thuộc tính {@code STATUS}, giá trị là trạng thái mới; trạng thái tại t là dòng
 * gần nhất không sau t (FR-026a). Mỗi tài khoản một dòng mỗi thời điểm ({@link #uniqueKey}, thay {@code UNIQUE} cũ).
 */
public record AccountStatusDetails(String lockCause, Long auditLogId) {

    public static final HistoryTrack<AccountStatusDetails> TRACK =
            new HistoryTrack<>("M15.ACCOUNT", "STATUS", AccountStatusDetails.class);

    private static final DateTimeFormatter MICROS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC);

    /** {@code M15.ASH:<tài khoản>:<thời điểm UTC tới micro giây>}, cùng dạng với dòng {@code V002_95} chép sang. */
    public static String uniqueKey(long accountId, Instant at) {
        return "M15.ASH:" + accountId + ":" + MICROS.format(at);
    }
}
