package com.eldercare.backend.roombedallocation.internal.bed.domain;

/** Thông tin gốc của lần xác nhận sẵn sàng, giữ nguyên tầng lúc ghi nhận. */
public record ReadinessCorrectionOriginal(
        long id,
        long bedId,
        long floorId,
        Long actorId,
        String event,
        Boolean disinfected,
        Boolean ppeUsed,
        String note) {}
