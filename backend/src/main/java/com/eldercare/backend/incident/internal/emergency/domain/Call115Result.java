package com.eldercare.backend.incident.internal.emergency.domain;

/** Kết quả gọi 115 do người bấm chọn (FR-047k); {@code progressionId} là diễn biến {@code CALL_EMERGENCY} đã thêm. */
public record Call115Result(String result, Long progressionId) {}
