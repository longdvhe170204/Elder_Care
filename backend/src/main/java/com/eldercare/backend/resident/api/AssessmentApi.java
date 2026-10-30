package com.eldercare.backend.resident.api;

import com.eldercare.backend.resident.api.dto.AssessmentSummary;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Đánh giá cho 004, 005, 007 (không có điểm hay mức đề xuất). */
public interface AssessmentApi {

    /** Lưu ý chăm sóc hiện hành, cho thẻ người cao tuổi và thẻ khẩn cấp (FR-038). */
    Map<Long, Optional<String>> currentCareNote(Collection<Long> residentIds);

    /** 004 FR-004 (a). */
    Optional<AssessmentSummary> latestConfirmed(long residentId);
}
