package com.eldercare.backend.administration.auditlog.api;

/**
 * Căn cứ của thay đổi do hệ thống thực hiện (FR-045): mã quy tắc (BR hoặc CFG), yêu cầu phê duyệt, bản ghi nguồn,
 * lần chạy của Bộ lập lịch. Cần ít nhất một thành phần.
 */
public record Basis(String ruleCode, Long approvalRequestId, Long sourceRecordId, Long schedulerRunId) {

    public Basis {
        if (ruleCode == null && approvalRequestId == null && sourceRecordId == null && schedulerRunId == null) {
            throw new IllegalArgumentException("Căn cứ cần ít nhất một thành phần (FR-045)");
        }
    }
}
