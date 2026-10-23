package com.cambae.backend.m01.api.dto;

import java.time.LocalDate;

/**
 * Nhận định của Bác sĩ gắn với chính phiếu nguyện vọng, không phải dấu năng lực của người cao tuổi (FR-023h):
 * {@code supportMethod} là cách đã hỗ trợ người cao tuổi hiểu thông tin, {@code reviewDate} là ngày xem lại.
 */
public record SelfDeterminationAssessment(boolean canSelfDetermine, String supportMethod, LocalDate reviewDate) {}
