package com.cambae.backend.m15.account.api;

/** Lớp kiểm quyền không thỏa (BR-M15-01; 002 research A-10, FR-041b). */
public enum FailedLayer {
    ROLE,
    SCOPE,
    LEGAL,
    SOURCE_DATA
}
