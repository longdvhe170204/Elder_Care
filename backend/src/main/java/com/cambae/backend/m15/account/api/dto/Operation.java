package com.cambae.backend.m15.account.api.dto;

/**
 * Loại thao tác (mục 19.2; 002 research A-04): X, P → {@code VIEW}; T → {@code CREATE}, {@code UPDATE} và {@code CONFIRM}
 * khi dòng có ghi nhận; dòng chuyên môn thêm {@code PROFESSIONAL}; D → {@code APPROVE}; dòng chốt kỳ thêm {@code FINALIZE}.
 */
public enum Operation {
    VIEW,
    CREATE,
    UPDATE,
    CONFIRM,
    APPROVE,
    FINALIZE,
    PROFESSIONAL
}
