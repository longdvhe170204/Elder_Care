package com.eldercare.backend.billing.api.dto;

import java.time.LocalDate;

/**
 * Chứng từ (tên logic {@code ChungTuView}; FR-038): mã phiếu hoặc mã bảng, kỳ của bảng chi phí (rỗng với bảng quyết
 * toán), phiên bản (rỗng với phiếu báo phí), tệp PDF và Excel ({@code xlsxFileId} rỗng với bảng quyết toán).
 */
public record BalanceDocumentView(
        BalanceDocumentType type,
        long id,
        String code,
        LocalDate date,
        String period,
        Integer version,
        long pdfFileId,
        Long xlsxFileId) {}
