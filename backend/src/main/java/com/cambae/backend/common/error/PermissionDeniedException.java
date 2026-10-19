package com.cambae.backend.common.error;

import org.springframework.http.HttpStatus;

/** Không có quyền thực hiện lệnh: 403 (BR-M15-01). */
public class PermissionDeniedException extends BusinessException {

    public PermissionDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", message);
    }
}
