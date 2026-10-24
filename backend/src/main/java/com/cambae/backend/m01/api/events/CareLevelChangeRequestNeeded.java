package com.cambae.backend.m01.api.events;

import com.cambae.backend.m01.api.dto.CareLevel;

/** Cần lập yêu cầu đổi mức, lần đầu; 009 báo Hành chính, mức Trung bình (FR-036). */
public record CareLevelChangeRequestNeeded(long residentId, long assessmentId, CareLevel newLevel) {}
