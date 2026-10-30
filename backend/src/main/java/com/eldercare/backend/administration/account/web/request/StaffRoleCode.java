package com.eldercare.backend.administration.account.web.request;

import com.eldercare.backend.common.Role;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;

/** Mã vai trò nhân viên (schema {@code StaffRoleCode}). */
@Schema(name = "StaffRoleCode")
public enum StaffRoleCode {
    AC_01(Role.NURSING_HOME_MANAGER),
    AC_02(Role.FLOOR_MANAGER),
    AC_03(Role.DOCTOR),
    AC_04(Role.NURSING_STAFF),
    AC_05(Role.CAREGIVER),
    AC_07(Role.KITCHEN_STAFF),
    AC_09(Role.OFFICE_STAFF),
    AC_14(Role.ACCOUNTANT);

    private final Role role;

    StaffRoleCode(Role role) {
        this.role = role;
    }

    /** Vai trò hệ thống của mã. */
    public Role role() {
        return role;
    }

    @JsonValue
    public String code() {
        return role.code();
    }

    @JsonCreator
    public static StaffRoleCode of(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không phải vai trò nhân viên: " + code));
    }
}
