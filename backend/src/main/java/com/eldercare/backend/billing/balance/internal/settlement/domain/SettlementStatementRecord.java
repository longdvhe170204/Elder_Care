package com.eldercare.backend.billing.balance.internal.settlement.domain;

/**
 * Nội dung một phiên bản bảng quyết toán ở {@code record_event} (017 data-model mục 6; FR-034, DBR-38). {@code remaining}
 * có dấu (âm còn nợ, dương còn phải hoàn), khác 0 chỉ khi có ngoại lệ. {@code lastTransactionId} là giao dịch Đã xác nhận
 * mới nhất lúc xác nhận: giao dịch Đã xác nhận có mã lớn hơn làm ý xác nhận Chưa đạt (không còn Chờ duyệt lúc xác nhận nên
 * mã tăng theo thứ tự xác nhận). {@code data} là số liệu chụp để dựng PDF.
 */
public record SettlementStatementRecord(
        int version,
        SettlementRules.Basis basis,
        boolean hasException,
        long remaining,
        Long lastTransactionId,
        long pdfFileId,
        long confirmedBy,
        SettlementStatementData data) {}
