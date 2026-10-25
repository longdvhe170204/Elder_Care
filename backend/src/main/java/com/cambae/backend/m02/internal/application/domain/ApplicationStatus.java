package com.cambae.backend.m02.internal.application.domain;

/** Trạng thái lượt đăng ký tiếp nhận (FR-002; glossary mục 10.3). */
public enum ApplicationStatus {
    IN_PROGRESS,
    WAITING,
    ADMITTED,
    CANCELLED;

    /** Đang xử lý hoặc Đang chờ: lượt còn sửa được và là lượt mở duy nhất của người cao tuổi (FR-001). */
    public boolean isOpen() {
        return this == IN_PROGRESS || this == WAITING;
    }
}
