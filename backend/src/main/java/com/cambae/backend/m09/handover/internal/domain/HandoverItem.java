package com.cambae.backend.m09.handover.internal.domain;
/** Nội dung gốc của mục, không lưu trạng thái nguồn hiện tại thay cho snapshot. */
public record HandoverItem(
        long id,
        long handoverId,
        String group,
        String sourceType,
        String sourceId,
        Long residentId,
        Long roomId,
        String description,
        String note,
        boolean resolved,
        String section,
        String statusAtFinalization,
        String noteAuthor) {}
