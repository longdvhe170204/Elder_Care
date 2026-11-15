package com.eldercare.backend.admission.api.dto;

import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.LocalDate;

/**
 * Một lần đổi loại lưu trú (016 FR-023; chủ dự án duyệt 2026-11-11): phiên bản nội dung của hợp đồng đã ký có loại lưu trú
 * khác phiên bản liền trước, hiệu lực từ {@code effectiveOn}.
 */
public record ResidenceTypeChangeView(
        long residentId, long contractId, ResidenceType from, ResidenceType to, LocalDate effectiveOn) {}
