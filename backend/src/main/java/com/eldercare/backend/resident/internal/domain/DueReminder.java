package com.eldercare.backend.resident.internal.domain;

/** Một dòng {@code periodic_reminder} đã tới lượt nhắc. */
public record DueReminder(long residentId, long milestoneId, int reminderCount) {}
