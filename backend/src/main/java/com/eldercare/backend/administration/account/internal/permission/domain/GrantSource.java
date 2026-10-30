package com.eldercare.backend.administration.account.internal.permission.domain;

/** Nguồn của một quyền đã tính: ma trận theo vai trò, nhiệm vụ (US4, T053), quyền gán thêm (glossary mục 7.3). */
public enum GrantSource {
    ROLE,
    DUTY,
    ADDITIONAL_GRANT
}
