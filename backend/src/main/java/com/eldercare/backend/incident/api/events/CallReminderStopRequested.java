package com.eldercare.backend.incident.api.events;

/** Dừng nhắc gọi của đúng nguồn khi sự cố hạ khỏi Khẩn cấp (BR-M05-22). */
public record CallReminderStopRequested(String sourceRecordType, long sourceRecordId) {}
