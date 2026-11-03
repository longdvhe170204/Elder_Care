package com.eldercare.backend.care.internal.task.domain;

import java.util.List;

public record CareTaskValidation(CareTask task, List<String> codes) {
    public CareTaskValidation {
        codes = List.copyOf(codes);
    }
}
