package com.eldercare.backend.resident.api;

import com.eldercare.backend.resident.api.dto.ReassessmentStatus;
import java.util.Collection;
import java.util.Map;

/** Trạng thái đánh giá lại cho 016 FR-044. */
public interface ReassessmentApi {

    Map<Long, ReassessmentStatus> status(Collection<Long> residentIds);
}
