package com.eldercare.backend.common;

import com.eldercare.backend.common.error.ReasonRequiredException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Lý do của lệnh: không rỗng, không chỉ có khoảng trắng, tối đa 2000 ký tự; sai thì 422 REASON_REQUIRED (FR-044). */
public record Reason(String value) {

    public static final int MAX_LENGTH = 2000;

    public Reason {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new ReasonRequiredException();
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Reason of(String value) {
        return new Reason(value);
    }

    @JsonValue
    @Override
    public String value() {
        return value;
    }
}
