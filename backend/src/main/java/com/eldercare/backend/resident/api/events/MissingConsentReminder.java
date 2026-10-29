package com.eldercare.backend.resident.api.events;

/** Nhắc thiếu bản đồng ý; 009 báo Hành chính, mức Nhẹ (FR-012). */
public record MissingConsentReminder(long residentId, int reminderNumber) {}
