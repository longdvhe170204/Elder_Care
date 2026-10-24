package com.cambae.backend.m03.internal.allocation.domain;
/** Bản amendment đã commit từ public view của 000, dùng cho before event trong parent RR. */
public record AllocationCorrectionSnapshot(long correctionId, String content) {}
