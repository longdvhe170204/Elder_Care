package com.eldercare.backend.administration.shared.internal.service;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Nội dung {@code JSON} của dòng bảng dùng chung là record của module sở hữu loại: kiểm bằng Bean Validation khi ghi, đọc
 * ghi bằng Jackson (thay JSON Schema theo loại của research R-15, chủ dự án duyệt 2026-10-30, plan 000).
 */
@Component
class SharedContent {

    private final JsonMapper json;
    private final Validator validator;

    SharedContent(JsonMapper json, Validator validator) {
        this.json = json;
        this.validator = validator;
    }

    /** 422 {@code INVALID_VALUE} khi nội dung sai ràng buộc của record; {@code null} ghi {@code NULL}. */
    <T> String write(T content) {
        if (content == null) {
            return null;
        }
        Set<ConstraintViolation<T>> violations = validator.validate(content);
        if (!violations.isEmpty()) {
            throw new InvalidValueException(violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return json.writeValueAsString(content);
    }

    <T> T read(Class<T> type, String value) {
        return value == null || type == Void.class ? null : json.readValue(value, type);
    }

    static ConditionNotMetException condition(String code, String description) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, description)));
    }
}
