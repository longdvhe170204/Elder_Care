package com.eldercare.backend.staffing.api.dto;

import java.time.LocalDate;

/**
 * Kiểu đọc giữa module, theo 008 contracts mục 1.3/1.4; không chứa thông tin liên hệ hoặc lý do riêng tư.
 * {@code credentialGroup} ({@code PROFESSIONAL_LICENSE}, {@code TRAINING}) cho 016 FR-020 tách mốc CFG-M06-02 và
 * CFG-M09-03 (luật "Đổi api", chủ dự án duyệt 2026-10-27).
 */
public record ExpiringCredentialView(
        long credentialId, long staffId, String credentialType, LocalDate expiryDate, String credentialGroup) {}
