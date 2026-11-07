package com.eldercare.backend.incident.internal.critical.domain;

import com.eldercare.backend.resident.api.dto.RecordEndOfLifeWish;
import java.time.Instant;
import java.util.function.LongFunction;

/**
 * Lệnh ghi kết quả xác nhận lại (FR-050c). {@code newWish} dựng thân lệnh nguyện vọng của 001 theo mã người cao tuổi của
 * cảnh báo; bắt buộc khi {@code CHANGED}.
 */
public record WishReconfirmationCommand(
        Long contactedFamilyMemberId,
        String contactedName,
        ContactChannel channel,
        Instant contactedAt,
        ReconfirmationResult result,
        LongFunction<RecordEndOfLifeWish> newWish) {}
