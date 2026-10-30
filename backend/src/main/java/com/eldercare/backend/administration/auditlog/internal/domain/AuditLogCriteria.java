package com.eldercare.backend.administration.auditlog.internal.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Điều kiện tra cứu nhật ký, mới trước; trường {@code null} (hoặc rỗng) là không lọc. {@code entityTypes} gồm loại cũ ánh
 * xạ về loại đang tra (research R-15 mục 5); con trỏ là thời điểm, id của dòng cuối trang trước.
 */
public record AuditLogCriteria(
        Collection<String> entityTypes,
        String entityId,
        Long actorId,
        List<String> actions,
        Instant from,
        Instant to,
        Boolean selfApproved,
        LocalDateTime afterOccurredAt,
        Long afterId) {}
