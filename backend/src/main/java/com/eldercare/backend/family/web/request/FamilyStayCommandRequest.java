package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.Reason;
import java.time.Instant;
import java.util.Map;

/**
 * Thân lệnh trên lượt ở lại (FR-045): lý do (từ chối, hủy, gia hạn), ý kiến người cùng phòng (xác nhận), thời gian kết
 * thúc dự kiến mới (gia hạn), thời điểm thực tế (bắt đầu, kết thúc; trống là lúc ghi).
 */
public record FamilyStayCommandRequest(
        Reason reason, Map<String, Object> roommateOpinion, Instant newExpectedEndAt, Instant at) {

    public static final FamilyStayCommandRequest EMPTY = new FamilyStayCommandRequest(null, null, null, null);
}
