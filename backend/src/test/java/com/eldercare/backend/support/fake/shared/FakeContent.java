package com.eldercare.backend.support.fake.shared;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/** Phần dùng chung của các bản giả bảng dùng chung: kiểm nội dung như bản thật, cấp id. */
final class FakeContent {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    static final AtomicLong IDS = new AtomicLong(1_000_000);

    static <T> T valid(T content) {
        if (content == null) {
            return null;
        }
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(content);
        if (!violations.isEmpty()) {
            throw new InvalidValueException(violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; ")));
        }
        return content;
    }

    static ConditionNotMetException condition(String code, String description) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, description)));
    }

    private FakeContent() {}
}
