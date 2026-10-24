package com.cambae.backend.m01.api.events;

/** Nhắc thiếu phiếu nguyện vọng; 009 báo Bác sĩ, Điều dưỡng phụ trách, mức Nhẹ (FR-023d). */
public record MissingEndOfLifeWishReminder(long residentId, int reminderNumber) {}
