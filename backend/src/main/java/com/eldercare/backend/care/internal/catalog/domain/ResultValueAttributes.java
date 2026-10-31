package com.eldercare.backend.care.internal.catalog.domain;

import jakarta.validation.constraints.*;

/** Mã dài tới100ký tự được giữ trong JSON,không cắt theo code40 của bảng chung. */
public record ResultValueAttributes(
        @NotNull @Pattern(regexp = "SLEEP_QUALITY|MOOD|BEHAVIOR")
        String type,

        @NotNull @Size(max = 100) String code,
        @Pattern(regexp = "INSOMNIA|NEGATIVE") String ruleGroup,
        LegacyCatalogId legacy) {}
