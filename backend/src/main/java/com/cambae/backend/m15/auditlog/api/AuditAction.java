package com.cambae.backend.m15.auditlog.api;

/**
 * Hành động ghi vào nhật ký, do module sở hữu khai báo trong danh mục hành động của mình. {@code requiresReason} theo
 * danh sách FR-044: lệnh nhóm 2, đính chính, từ chối, hủy, tự duyệt, đổi tham số, xuất báo cáo có định danh.
 */
public record AuditAction(String code, boolean requiresReason) {

    /**
     * Bản tóm tắt một đợt loại bỏ dữ liệu hết hạn (FR-051). Nhật ký ghi bản này với {@code exempt_from_purge = TRUE}:
     * job không loại bỏ nó và trigger chặn xóa.
     */
    public static final AuditAction EXPIRED_DATA_PURGED = new AuditAction("EXPIRED_DATA_PURGED", false);

    public AuditAction {
        if (code == null || code.isBlank() || code.length() > 40) {
            throw new IllegalArgumentException("Mã hành động 1–40 ký tự");
        }
    }
}
