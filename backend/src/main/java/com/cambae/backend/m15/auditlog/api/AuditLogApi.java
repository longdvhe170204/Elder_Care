package com.cambae.backend.m15.auditlog.api;

import java.util.List;

/** Ghi nhật ký hệ thống (contracts/module-interfaces.md mục 1.2; FR-042 → FR-047). */
public interface AuditLogApi {

    /** Ghi trong transaction của lệnh (Propagation.MANDATORY): cùng commit hoặc cùng rollback với thay đổi (FR-047). */
    void write(AuditLogEntry entry);

    /**
     * Ghi trong transaction riêng (REQUIRES_NEW), giữ lại dù lệnh gọi bị rollback. Chỉ dùng cho lần áp dụng yêu cầu bị
     * lỗi (FR-037b) và bản tóm tắt đợt loại bỏ dữ liệu (FR-051).
     */
    void writeOutsideTransaction(AuditLogEntry entry);

    /**
     * Ghi trong transaction của lệnh với người thực hiện "khởi tạo hệ thống" ({@code actor_type = BOOTSTRAP}, không có
     * tài khoản, vai trò); bắt buộc căn cứ. Chỉ công cụ khởi tạo Quản lý viện đầu tiên dùng (002 T016; BR-M15-22).
     */
    void writeAsBootstrap(AuditLogEntry entry);

    /**
     * Ghi trong transaction của lệnh với người thực hiện "chưa đăng nhập" ({@code actor_type = ANONYMOUS}, không có tài
     * khoản, vai trò): chỉ lần đăng nhập thất bại (002 FR-015, research A-03).
     */
    void writeAnonymous(AuditLogEntry entry);

    /**
     * Bản ghi của một hành động trên một loại đối tượng, mới nhất trước, tối đa {@link #RECORDED_LIMIT} dòng
     * ({@code entityId}, {@code actorAccountId} rỗng là không lọc). Cho module ghi đọc lại hành động của mình, ví dụ lần
     * xuất báo cáo của 016 (FR-062; "Đổi api" 2026-10-24). Không kiểm quyền và không che trường: module gọi tự kiểm.
     */
    List<AuditLogRecord> recorded(AuditAction action, String entityType, String entityId, Long actorAccountId);

    int RECORDED_LIMIT = 200;
}
