package com.cambae.backend.m09.handover.internal.domain;
/** Chỉ trường ghi của HandoverItem; không nhận section hay trạng thái nguồn từ client. */
public record HandoverItemInput(
        String group,
        String sourceType,
        String sourceId,
        Long residentId,
        Long roomId,
        String description,
        String note,
        boolean resolved) {}
