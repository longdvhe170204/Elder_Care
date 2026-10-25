package com.cambae.backend.m02.internal.contract.domain;

import com.cambae.backend.m02.api.dto.ContractStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Hợp đồng lưu trú (data-model mục 5; FR-019 → FR-029). {@code currentEndDate} là ngày kết thúc sau phụ lục gia hạn;
 * {@code overdueSince} là ngày bắt đầu dấu "quá hạn hợp đồng" (FR-029). {@code awaitingTermsApproval} khóa nội dung trong khi
 * yêu cầu duyệt điều khoản khác chuẩn Chờ duyệt; {@code termsApprovalRequestId} là yêu cầu đó hoặc yêu cầu đã duyệt cho
 * {@code contentHash}.
 */
public record Contract(
        long id,
        String contractNumber,
        long residentId,
        ContractContent content,
        LocalDate currentEndDate,
        Long signingRepresentativeId,
        LocalDate signedDate,
        Long scanFileId,
        String contentHash,
        boolean awaitingTermsApproval,
        Long termsApprovalRequestId,
        ContractStatus status,
        String terminationReason,
        LocalDate overdueSince,
        Instant managerNotifiedOverdueAt,
        int version,
        long createdBy,
        Instant createdAt) {

    public Contract withContent(ContractContent newContent) {
        return new Contract(
                id,
                contractNumber,
                residentId,
                newContent,
                currentEndDate,
                signingRepresentativeId,
                signedDate,
                scanFileId,
                contentHash,
                awaitingTermsApproval,
                termsApprovalRequestId,
                status,
                terminationReason,
                overdueSince,
                managerNotifiedOverdueAt,
                version,
                createdBy,
                createdAt);
    }
}
