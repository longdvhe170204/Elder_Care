package com.eldercare.backend.staffing.internal.swap.domain;

import java.time.Instant;

/** Ca của người nhận thỏa FR-019: chỉ ngày, giờ, mẫu ca (FR-020). */
public record RecipientShift(long shiftId, Instant startsAt, Instant endsAt, long shiftTemplateId) {}
