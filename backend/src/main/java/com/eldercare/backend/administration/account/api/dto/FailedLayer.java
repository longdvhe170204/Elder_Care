package com.eldercare.backend.administration.account.api.dto;

/** Lớp kiểm quyền không thỏa (BR-M15-01; 002 research A-10, FR-041b). */
public enum FailedLayer {
    ROLE,
    SCOPE,
    LEGAL,
    SOURCE_DATA
}
