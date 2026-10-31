package com.eldercare.backend.health.api.dto;

/** Số liên hệ của viện; phoneNumber trống khi chưa được cấu hình (FR-070). */
public record FacilityContactView(String phoneNumber, String note, int version) {}
