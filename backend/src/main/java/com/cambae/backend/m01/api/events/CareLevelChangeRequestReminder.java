package com.cambae.backend.m01.api.events;

import com.cambae.backend.m01.api.dto.CareLevel;

/** Nhắc lập yêu cầu đổi mức; 009 báo Hành chính, mức Nhẹ. Khóa sự kiện (residentId, assessmentId, reminderNumber). */
public record CareLevelChangeRequestReminder(
        long residentId, long assessmentId, CareLevel newLevel, int reminderNumber) {}
