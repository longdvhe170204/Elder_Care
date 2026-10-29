package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.resident.api.dto.CareLevel;

/** Đánh giá đầu vào được xác nhận; 004 nghe (004 FR-025b; FR-033). */
public record InitialAssessmentConfirmed(long residentId, long assessmentId, CareLevel careLevel) {}
