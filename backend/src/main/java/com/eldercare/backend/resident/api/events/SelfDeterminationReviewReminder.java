package com.eldercare.backend.resident.api.events;

/** Nhắc xem lại nhận định tự quyết (FR-023i, Q-360); 009 báo Bác sĩ đã nhận định, Điều dưỡng phụ trách, mức Nhẹ. */
public record SelfDeterminationReviewReminder(long residentId, long wishId) {}
