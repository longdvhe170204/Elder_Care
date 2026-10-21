package com.cambae.backend.m01.api.dto;

import java.util.Optional;

/** Lệnh ghi phiên bản phiếu nguyện vọng do module khác gọi (007 xác nhận lại khi nguy kịch). */
public record RecordEndOfLifeWish(
        long residentId,
        WishChoice choice,
        String firstContact,
        String note,
        WishSignerType signerType,
        Optional<Long> signerFamilyMemberId) {}
