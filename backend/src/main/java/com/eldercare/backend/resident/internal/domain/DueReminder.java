package com.eldercare.backend.resident.internal.domain;

/** Một việc nhắc định kỳ (dấu {@code reminder_marker}) đã tới lượt nhắc. */
public record DueReminder(long residentId, long milestoneId, int reminderCount) {}
