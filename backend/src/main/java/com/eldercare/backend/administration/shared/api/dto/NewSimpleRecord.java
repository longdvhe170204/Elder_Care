package com.eldercare.backend.administration.shared.api.dto;

import java.time.LocalDate;

/** Đối tượng nhỏ mới của {@code simple_record}; {@code lock} khóa dòng ngay (nhóm 3 hoặc tạo ở trạng thái đã chốt). */
public record NewSimpleRecord<T>(
        Long residentId,
        EntityRef parent,
        String status,
        T content,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String uniqueKey,
        long createdBy,
        boolean lock) {}
