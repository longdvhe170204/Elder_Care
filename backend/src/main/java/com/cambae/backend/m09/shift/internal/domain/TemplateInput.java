package com.cambae.backend.m09.shift.internal.domain;

import java.time.LocalTime;

/** Nội dung mẫu ca; giờ do Quản lý viện cấu hình, không dùng giá trị cố định trong sản phẩm. */
public record TemplateInput(
        String name,
        LocalTime startTime,
        LocalTime endTime,
        String scopeType,
        Boolean handoverRequired,
        boolean active) {}
