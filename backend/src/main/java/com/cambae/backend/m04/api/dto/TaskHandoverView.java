package com.cambae.backend.m04.api.dto;

import java.time.Instant;

/** Nguồn bàn giao 005: trạng thái hiện hành; chỉ đọc, không tự lập mục. */
public record TaskHandoverView(
        long id, long residentId, Long roomId, String description, Instant plannedAt, String status, String level) {}
