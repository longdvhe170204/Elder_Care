package com.cambae.backend.m15.account.api;

import java.util.Optional;

/**
 * Căn cứ cho phép của {@link PermissionResult}: vai trò, nhiệm vụ (kèm loại), ngoại lệ phạm vi sự cố FR-044a, Bộ lập
 * lịch FR-044 (002 research A-06, A-10). Module chép vào nhật ký của lệnh.
 */
public record PermissionBasis(Kind kind, Optional<Duty> duty) {

    public enum Kind {
        ROLE,
        DUTY,
        OUT_OF_SCOPE,
        SCHEDULER
    }

    public static final PermissionBasis ROLE = new PermissionBasis(Kind.ROLE, Optional.empty());
    public static final PermissionBasis OUT_OF_SCOPE = new PermissionBasis(Kind.OUT_OF_SCOPE, Optional.empty());
    public static final PermissionBasis SCHEDULER = new PermissionBasis(Kind.SCHEDULER, Optional.empty());

    public static PermissionBasis duty(Duty duty) {
        return new PermissionBasis(Kind.DUTY, Optional.of(duty));
    }
}
