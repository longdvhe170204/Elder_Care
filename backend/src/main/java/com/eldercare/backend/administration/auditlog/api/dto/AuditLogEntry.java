package com.eldercare.backend.administration.auditlog.api.dto;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.ReasonRequiredException;
import java.time.Instant;
import java.util.Map;

/**
 * Một bản ghi nhật ký (FR-044, data-model mục 5). Người thực hiện, vai trò và thời điểm do {@link AuditLogApi} đặt khi
 * ghi. {@code before}, {@code after} chỉ gồm trường đã đổi; để trống với hành động không đổi dữ liệu (FR-043a).
 */
public record AuditLogEntry(
        AuditAction action,
        String entityType,
        String entityId,
        Long residentId,
        Map<String, Object> before,
        Map<String, Object> after,
        String reason,
        Basis basis,
        Map<String, Object> details,
        Instant deviceTime) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private AuditAction action;
        private String entityType;
        private String entityId;
        private Long residentId;
        private Map<String, Object> before;
        private Map<String, Object> after;
        private Reason reason;
        private Basis basis;
        private Map<String, Object> details;
        private Instant deviceTime;

        private Builder() {}

        public Builder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public Builder entity(String type, Object id) {
            this.entityType = type;
            this.entityId = id == null ? null : String.valueOf(id);
            return this;
        }

        /** Người cao tuổi của hồ sơ liên quan, để lưu giữ cùng hồ sơ (FR-050). */
        public Builder resident(Long residentId) {
            this.residentId = residentId;
            return this;
        }

        public Builder before(Map<String, Object> before) {
            this.before = before;
            return this;
        }

        public Builder after(Map<String, Object> after) {
            this.after = after;
            return this;
        }

        public Builder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public Builder basis(Basis basis) {
            this.basis = basis;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        /** Thời điểm trên thiết bị của bản ghi ngoại tuyến (DBR-25). */
        public Builder deviceTime(Instant deviceTime) {
            this.deviceTime = deviceTime;
            return this;
        }

        /** Báo lỗi khi thiếu hành động, đối tượng, hoặc hành động bắt buộc lý do mà thiếu lý do (FR-044). */
        public AuditLogEntry build() {
            if (action == null || entityType == null || entityId == null) {
                throw new IllegalStateException("Bản ghi nhật ký cần hành động và đối tượng");
            }
            if (action.requiresReason() && reason == null) {
                throw new ReasonRequiredException();
            }
            return new AuditLogEntry(
                    action,
                    entityType,
                    entityId,
                    residentId,
                    before,
                    after,
                    reason == null ? null : reason.value(),
                    basis,
                    details,
                    deviceTime);
        }
    }
}
