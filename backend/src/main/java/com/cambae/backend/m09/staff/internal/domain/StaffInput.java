package com.cambae.backend.m09.staff.internal.domain;

import java.time.LocalDate;

/** Nội dung mô tả được phép ghi; không nhận trạng thái hoặc liên kết tài khoản từ HTTP. */
public record StaffInput(
        String fullName,
        String position,
        String specialty,
        LocalDate startDate,
        String phoneNumber,
        String email,
        Long departmentId) {}
