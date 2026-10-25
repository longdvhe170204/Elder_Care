package com.cambae.backend.m02.api.dto;

/** Một bước của tiến trình tiếp nhận (FR-003): {@code name} là mã bước, {@code done} kèm căn cứ. */
public record ProgressStepView(String name, boolean done, String basis) {}
