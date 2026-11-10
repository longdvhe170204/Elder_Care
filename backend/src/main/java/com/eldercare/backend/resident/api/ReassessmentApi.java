package com.eldercare.backend.resident.api;

import com.eldercare.backend.resident.api.dto.ReassessmentStatus;
import java.util.Collection;
import java.util.Map;

/** Trạng thái đánh giá lại cho 016 FR-044; báo đánh giá lại sau sự cố cho 007 (A duyệt 2026-11-05). */
public interface ReassessmentApi {

    Map<Long, ReassessmentStatus> status(Collection<Long> residentIds);

    /**
     * Báo cần đánh giá lại sau sự cố Ngã hoặc mức Trung bình trở lên (001 FR-024, research H-11), cùng transaction lệnh của
     * 007; gọi lại cùng sự cố không báo thêm. Trả {@code true} khi có lần báo mới.
     */
    boolean notifyAfterIncident(long residentId, long incidentId);
}
