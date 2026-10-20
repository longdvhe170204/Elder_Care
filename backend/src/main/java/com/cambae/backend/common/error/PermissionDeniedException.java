package com.cambae.backend.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Không có quyền thực hiện lệnh: 403 (BR-M15-01). Bản có {@code properties} cho interceptor quyền của 002: thêm vào thân
 * lỗi các trường {@code failedLayer}, {@code license} (002 FR-043, research A-10).
 */
public class PermissionDeniedException extends BusinessException {

    public PermissionDeniedException(String message) {
        this("PERMISSION_DENIED", message, Map.of());
    }

    /** {@code errorCode}: {@code PERMISSION_DENIED} hoặc {@code LEGAL_CONDITION_NOT_MET} (glossary mục 5). */
    public PermissionDeniedException(String errorCode, String message, Map<String, Object> properties) {
        super(HttpStatus.FORBIDDEN, errorCode, message, properties);
    }
}
