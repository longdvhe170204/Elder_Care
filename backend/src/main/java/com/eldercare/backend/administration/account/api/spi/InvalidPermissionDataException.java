package com.eldercare.backend.administration.account.api.spi;

/**
 * Module nguồn ném khi dữ liệu cần cho kiểm quyền thiếu hoặc mâu thuẫn; 002 từ chối với lớp {@code SOURCE_DATA} và báo
 * Quản lý viện (FR-041b; 002 research A-10).
 */
public class InvalidPermissionDataException extends RuntimeException {

    private final String source;
    private final String errorKind;
    private final Long recordId;

    public InvalidPermissionDataException(String source, String errorKind, Long recordId) {
        super(source + ": " + errorKind + (recordId == null ? "" : " (" + recordId + ")"));
        this.source = source;
        this.errorKind = errorKind;
        this.recordId = recordId;
    }

    public String source() {
        return source;
    }

    public String errorKind() {
        return errorKind;
    }

    public Long recordId() {
        return recordId;
    }
}
