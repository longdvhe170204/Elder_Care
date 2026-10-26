package com.cambae.backend.m09.api.dto;

import java.time.LocalDate;

/** Kiểu đọc giữa module, theo 008 contracts mục 1.3/1.4; không chứa thông tin liên hệ hoặc lý do riêng tư. */
public record ExpiringCredentialView(long credentialId, long staffId, String credentialType, LocalDate expiryDate) {}
