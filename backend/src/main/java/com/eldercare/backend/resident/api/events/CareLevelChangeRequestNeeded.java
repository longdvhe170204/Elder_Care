package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.resident.api.dto.CareLevel;

/** Cần lập yêu cầu đổi mức, lần đầu; 009 báo Hành chính, mức Trung bình (FR-036). */
public record CareLevelChangeRequestNeeded(long residentId, long assessmentId, CareLevel newLevel) {}
